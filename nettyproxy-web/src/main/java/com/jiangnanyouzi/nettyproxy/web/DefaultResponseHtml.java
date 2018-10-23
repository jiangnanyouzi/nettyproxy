package com.jiangnanyouzi.nettyproxy.web;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.utils.HttpRequestUtils;
import com.jiangnanyouzi.nettyproxy.utils.HttpUtils;
import com.jiangnanyouzi.nettyproxy.utils.ResponseUtil;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultResponseHtml {

    private Pattern requestHeaderPattern = Pattern.compile("\\{requestHeader\\}(.*)\\{\\/requestHeader\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private Pattern responseHeaderPattern = Pattern.compile("\\{responseHeader\\}(.*)\\{\\/responseHeader\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private Pattern requestParamterPattern = Pattern.compile("\\{requestParamter\\}(.*)\\{\\/requestParamter\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private Pattern requestPattern = Pattern.compile("\\{request\\}(.*)\\{\\/request\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static Logger logger = LoggerFactory.getLogger(DefaultResponseHtml.class);
    private static String templateContent = null;

    static {
        try {
            URL url = DefaultRequestHtml.class.getClassLoader().getResource("content.html");
            if (url != null) {
                templateContent = IOUtils.toString(url, "UTF-8");
            }
        } catch (IOException e) {
            logger.error("error {}", e);
        }
    }


    public String getHtml(ResponseInfo responseInfo) {

        String content = templateContent;
        Matcher match = requestHeaderPattern.matcher(content);
        if (match.find()) {
            String foreachHtml = match.group(1);
            content = match.replaceAll(replaceHeaderTag(responseInfo.getFullHttpRequest().headers(), foreachHtml));
        }
        content = replaceRequestTag(responseInfo, content);
        content = replaceResponse(responseInfo, content);
        content = content.replaceAll("\\{port\\}", String.valueOf(ProxyConstant.PORT));
        content = content.replaceAll("\\{url\\}", Matcher.quoteReplacement(ResponseUtil.fixUrl(responseInfo.getFullHttpRequest(), responseInfo.isHttps())));
        content = content.replaceAll("\\{httpMethod\\}", responseInfo.getFullHttpRequest().method().toString());
        String statusCode = responseInfo.getFullHttpResponse() == null ? "-" : String.valueOf(responseInfo.getFullHttpResponse().status().code());
        content = content.replaceAll("\\{statusCode\\}", statusCode);
        return content;
    }

    private String replaceResponse(ResponseInfo responseInfo, String content) {

        Matcher match = responseHeaderPattern.matcher(content);
        if (match.find()) {
            String responseHeaderHtml = match.group(1);
            HttpHeaders httpHeaders;
            if (responseInfo.getFullHttpResponse() != null) {
                httpHeaders = responseInfo.getFullHttpResponse().headers();
                content = match.replaceAll(replaceHeaderTag(httpHeaders, responseHeaderHtml));
            } else {
                content = match.replaceAll("not response");
            }
        }
        try {
            content = content.replaceAll("\\{responseTxt\\}", Matcher.quoteReplacement(getResponseTxt(responseInfo)));
        } catch (IOException e) {
            logger.error("getResponseTxt error {}", e);
        }
        return content;
    }


    private String replaceRequestTag(ResponseInfo responseInfo, String content) {

        logger.info("Replace RequestTag..........");

        Matcher match = requestPattern.matcher(content);
        if (match.find()) {

            if (responseInfo.getRequest() != null) {
                return match.replaceAll(Matcher.quoteReplacement((String) responseInfo.getRequest()));
            }
            String requestHtml = match.group(1);
            Matcher paramterMatch = requestParamterPattern.matcher(requestHtml);
            if (paramterMatch.find()) {
                String paramterHtml = paramterMatch.group(1);
                requestHtml = paramterMatch.replaceAll(Matcher.quoteReplacement(replaceRequestParamterTag(responseInfo, paramterHtml)));
            }
            responseInfo.setRequest(requestHtml);
            content = match.replaceAll(Matcher.quoteReplacement(requestHtml));
        }
        return content;
    }


    private String replaceRequestParamterTag(ResponseInfo responseInfo, String foreachHtml) {

        logger.info("Replace RequestParamterTag..........");

        FullHttpRequest fullHttpRequest = responseInfo.getFullHttpRequest();
        fullHttpRequest.content().resetReaderIndex();
        StringBuilder row = new StringBuilder();
        if (fullHttpRequest.method() == HttpMethod.GET ||
                fullHttpRequest.method() == HttpMethod.POST) {
            Map<String, List<String>> paramters;
            if (fullHttpRequest.method() == HttpMethod.GET) {
                paramters = HttpRequestUtils.getRequestParameters(fullHttpRequest.uri());
            } else {
                paramters = HttpRequestUtils.getPostParamters(fullHttpRequest);
            }
            for (String s : paramters.keySet()) {
                List<String> valueList = paramters.get(s);
                String value = "";
                if (valueList.size() == 1) {
                    value = valueList.get(0);
                }
                if (valueList.size() > 1) {
                    value = valueList.toString();
                }
                String newHtml = foreachHtml.replaceAll("\\{name\\}", Matcher.quoteReplacement(s));
                newHtml = newHtml.replaceAll("\\{value\\}", Matcher.quoteReplacement(value));
                row.append(newHtml);
            }
        }
        if (row.length() <= 0) {
            row.append("request content is blank");
        }
        return row.toString();
    }

    private String replaceHeaderTag(HttpHeaders httpHeaders, String foreachHtml) {

        StringBuilder builder = new StringBuilder();
        for (String s : httpHeaders.names()) {
            String newHtml = foreachHtml.replaceAll("\\{name\\}", s);
            newHtml = newHtml.replaceAll("\\{value\\}", httpHeaders.get(s));
            builder.append(newHtml);
        }
        return builder.toString();
    }


    public String getResponseTxt(ResponseInfo responseInfo) throws IOException {

        logger.info("get reponse txt.............");
        if (responseInfo.getResponse() == null && responseInfo.getFullHttpResponse() == null) {

            return "connect fail";
        }

        if (responseInfo.getResponse() == null && HttpUtil.getContentLength(responseInfo.getFullHttpResponse()) > 0) {
            FullHttpResponse fullHttpResponse = responseInfo.getFullHttpResponse();
            String contentType = StringUtils.defaultIfBlank(fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE), "text/plain");
            String mimeType = HttpUtil.getMimeType(contentType).toString();
            fullHttpResponse.content().resetReaderIndex();
            if (!mimeType.contains("image")) {
                String text = ResponseUtil.getHttpResponseTxt(fullHttpResponse);
                String responseTxt = "<pre class='layui-code'>" + StringEscapeUtils.escapeHtml4(text) + "</pre>";
                responseInfo.setResponse(responseTxt);
            } else {
                String base64Prefix = "data:" + mimeType + ";base64,";
                String base64Txt;
                if (HttpHeaderValues.GZIP.toString().equalsIgnoreCase(fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_ENCODING))) {
                    base64Txt = base64Prefix + Base64.getEncoder().encodeToString(HttpUtils.unGzip((ByteBufUtil.getBytes(fullHttpResponse.content()))));
                } else {
                    base64Txt = base64Prefix + Base64.getEncoder().encodeToString(ByteBufUtil.getBytes(fullHttpResponse.content()));
                }
                String responseTxt = "<img src=\"" + base64Txt + "\">";
                responseInfo.setResponse(responseTxt);
            }
            ReferenceCountUtil.release(fullHttpResponse);
        }

        //content length is 0
        if (responseInfo.getResponse() == null) {
            responseInfo.setResponse("content length is 0");
        }

        return (String) responseInfo.getResponse();
    }

}
