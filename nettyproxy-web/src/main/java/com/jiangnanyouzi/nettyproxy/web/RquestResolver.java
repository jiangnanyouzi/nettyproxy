package com.jiangnanyouzi.nettyproxy.web;

import com.jiangnanyouzi.nettyproxy.client.ClientRequestInfo;
import com.jiangnanyouzi.nettyproxy.client.ProxyClient;
import com.jiangnanyouzi.nettyproxy.utils.HttpRequestUtils;
import com.jiangnanyouzi.nettyproxy.utils.HttpUtils;
import com.jiangnanyouzi.nettyproxy.utils.ResponseUtil;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by jiangnan on 2018/7/16.
 */
public class RquestResolver {

    public static final String Edit_REGEX = ".*/edit/request/(\\d+)";
    public static final String POST_REGEX = ".*/post/request.*";

    public static Pattern optionPattern = Pattern.compile("\\{foreach\\}(.*)\\{\\/foreach\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static Pattern requestHeaderPattern = Pattern.compile("\\{requestHeader\\}(.*)\\{\\/requestHeader\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    public static Pattern bodyPattern = Pattern.compile("\\{body\\}(.*)\\{\\/body\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static String requestTemplateContent;
    private static String requestHeaderContent;
    private static String optionContent;
    private static String bodyContent;
    private static Logger logger = LoggerFactory.getLogger(RquestResolver.class);

    private FullHttpRequest request;

    public RquestResolver(FullHttpRequest request) {
        this.request = request;
    }


    static {
        init();
    }

    public String resolve() {

        if (Pattern.matches(Edit_REGEX, request.uri())) {

            try {
                return buildHtml(request);
            } catch (Exception e) {
                return "error,<pre>" + ExceptionUtils.getStackTrace(e) + "</pre>";
            }
        }

        try {
            return reslovePost(request);
        } catch (Exception e) {
            return "{\"status\":0,\"errorMsg\":\"" + ExceptionUtils.getStackTrace(e) + "\"}";
        }
    }

    private String reslovePost(FullHttpRequest request) {
        Map<String, List<String>> paramters = HttpRequestUtils.getPostParamters(request);
        logger.info("paramters {}", paramters);
        if (!paramters.containsKey("names[]") ||
                !paramters.containsKey("values[]") ||
                !paramters.containsKey("uri") ||
                !paramters.containsKey("https")
                ) {

            return "{\"status\":0,\"errorMsg\":\"parameter error\"}";
        }

        List<String> names = paramters.get("names[]");
        List<String> values = paramters.get("values[]");
        if (names.size() != values.size()) {
            return "{\"status\":0,\"errorMsg\":\"names or values error\"}";
        }
        if (names.indexOf("Host") == -1) {
            return "{\"status\":0,\"errorMsg\":\"host is required\"}";
        }

        FullHttpRequest fullHttpRequest = generateHttpRequest(paramters);
        boolean https = Boolean.valueOf(paramters.get("https").get(0));

        String returnHtml = retryRequest(fullHttpRequest.copy(), https);
        return "{\"status\":1,\"returnHtml\":\"" + StringEscapeUtils.escapeJson(returnHtml) + "\"}";
    }

    private String retryRequest(FullHttpRequest fullHttpRequest, boolean https) {

        String hostTxt = fullHttpRequest.headers().get(HttpHeaderNames.HOST);
        String host = HttpUtils.getHost(hostTxt);
        int port = https ? 443 : 80;
        logger.info("Retry Request, host {} port {}", host, port);
        //send request
        ClientRequestInfo.Builder builder = new ClientRequestInfo.Builder().host(host).port(port).reserve(true);
        ClientRequestInfo clientRequestInfo = builder.https(https).msg(fullHttpRequest.copy()).build();
        ProxyClient proxyClient = new ProxyClient();
        proxyClient.setClientRequestInfo(clientRequestInfo);
        proxyClient.connectNewRemoteServer();
        return new ResponseHtml().saveAndReturnHtml(fullHttpRequest, clientRequestInfo);
    }

    private FullHttpRequest generateHttpRequest(Map<String, List<String>> paramters) {

        List<String> names = paramters.get("names[]");
        List<String> values = paramters.get("values[]");
        String uri = paramters.get("uri").get(0);
        String httpMethod = paramters.get("httpMethod").get(0);

        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        for (int i = 0, length = names.size(); i < length; i++) {
            logger.info("{} = {}", names.get(i), values.get(i));
            httpHeaders.add(names.get(i), values.get(i));
        }

        FullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.valueOf(httpMethod), uri);
        fullHttpRequest.headers().add(httpHeaders);

        //http content
        if (paramters.containsKey("body")) {
            HttpContent httpContent = new DefaultLastHttpContent();
            String bodyData = paramters.get("body").get(0);
            httpContent.content().writeBytes(bodyData.getBytes());
            fullHttpRequest.content().writeBytes(httpContent.content());
        }
        return fullHttpRequest;
    }

    private String buildHtml(FullHttpRequest request) throws IllegalAccessException, IOException {
        int id = ResponseUtil.getId(request.uri());
        logger.info("edit request,id {}", id);
        ResponseInfo responseInfo = ResponseUtil.getCorrectResponseInfo(id);
        if (responseInfo == null || responseInfo.getFullHttpRequest() == null) {
            return "<pre>error</pre>";
        }
        FullHttpRequest fullHttpRequest = responseInfo.getFullHttpRequest();
        fullHttpRequest.content().resetReaderIndex();
        HttpHeaders headers = fullHttpRequest.headers();
        String headersHtml = replaceHeadersTag(headers);
        String content = requestTemplateContent;
        String bodyHtml = replaceBodyTag(IOUtils.toString(ByteBufUtil.getBytes(fullHttpRequest.content()), "utf-8"));

        //replace {requestHeader}....{/requestHeader} {body}....{/body} {https} {uri} {httpMethod}
        content = content.replaceAll("\\{requestHeader\\}(?s)(.*)\\{\\/requestHeader\\}", Matcher.quoteReplacement(headersHtml));
        content = content.replaceAll("\\{body\\}(?s)(.*)\\{\\/body\\}", bodyHtml);
        content = content.replaceAll("\\{uri\\}", fullHttpRequest.uri());
        content = content.replaceAll("\\{https\\}", String.valueOf(responseInfo.isHttps()));
        content = content.replaceAll("\\{httpMethod\\}", fullHttpRequest.method().toString());
        return content;
    }


    private static void init() {
        URL url = RquestResolver.class.getClassLoader().getResource("request.html");
        if (url == null) {
            throw new IllegalArgumentException("sources request.html not find");
        }
        try {
            requestTemplateContent = IOUtils.toString(url, "UTF-8");
            Matcher match = requestHeaderPattern.matcher(requestTemplateContent);
            if (match.find()) {
                requestHeaderContent = match.group(1);
            }
            match = optionPattern.matcher(requestHeaderContent);
            if (match.find()) {
                optionContent = match.group(1);
            }
            match = bodyPattern.matcher(requestTemplateContent);
            if (match.find()) {
                bodyContent = match.group(1);
            }
        } catch (IOException ignored) {
            //ignored
            logger.warn("{}", ignored);
        }
    }

    public String defaultSelectHtml(String header, String optionHtml) throws IllegalAccessException {

        StringBuilder stringBuilder = new StringBuilder();
        List<Field> fieldList = FieldUtils.getAllFieldsList(HttpHeaderNames.class);
        for (Field field : fieldList) {

            String value = field.get(field.getName()).toString();
            String newHtml = optionHtml.replaceAll("\\{value\\}", ResponseUtil.toUpperCase(value));
            newHtml = newHtml.replaceAll("\\{name\\}", ResponseUtil.toUpperCase(value));
            if (header.equalsIgnoreCase(value)) {
                newHtml = newHtml.replaceAll("\\{selected\\}", "selected=''");
            } else {
                newHtml = newHtml.replaceAll("\\{selected\\}", "");
            }
            stringBuilder.append(newHtml);
        }

        return stringBuilder.toString();
    }


    public String replaceHeadersTag(HttpHeaders httpHeaders) throws IllegalAccessException {

        StringBuilder builder = new StringBuilder();
        for (String s : httpHeaders.names()) {
            String newHtml = requestHeaderContent.replaceAll("\\{value\\}", httpHeaders.get(s));
            newHtml = newHtml.replaceAll("\\{foreach\\}(?s)(.*)\\{\\/foreach\\}", Matcher.quoteReplacement(defaultSelectHtml(s, optionContent)));
            builder.append(newHtml);
        }
        return builder.toString();
    }

    public String replaceBodyTag(String content) throws IllegalAccessException {

        if (content.length() <= 0) {
            return "request content is blank";
        }

        return Matcher.quoteReplacement(bodyContent.replaceAll("\\{bodyContent\\}", content));

    }

}
