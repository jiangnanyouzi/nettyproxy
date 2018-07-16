package com.jiangnanyouzi.nettyproxy.web;

import com.jiangnanyouzi.nettyproxy.client.ClientRequestInfo;
import com.jiangnanyouzi.nettyproxy.client.ProxyClient;
import com.jiangnanyouzi.nettyproxy.config.WebProxyConstant;
import com.jiangnanyouzi.nettyproxy.utils.HttpRequestUtils;
import com.jiangnanyouzi.nettyproxy.utils.HttpUtils;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jiangnan on 2018/7/9.
 */
public class ResponseHtml {

    public static final String REGEX = ".*/get/request/(\\d+)";
    public static final String RETRY_REGEX = ".*/retry/request/(\\d+)";
    public static final String BLACK_REGEX = ".*/black/request/(\\d+)";
    public static final String DELETE_REGEX = ".*/delete/request.*";

    private static Pattern pattern = Pattern.compile(REGEX);
    private static Pattern retryPattern = Pattern.compile(RETRY_REGEX);
    private static Pattern blackPattern = Pattern.compile(BLACK_REGEX);

    private Logger logger = LoggerFactory.getLogger(getClass());

    public String buildHtml(FullHttpRequest request) {

        if (Pattern.matches(DELETE_REGEX, request.uri())) {
            return deleteRequest(request);
        }

        if (Pattern.matches(RETRY_REGEX, request.uri())) {
            return retryRequest(request);
        }

        if (Pattern.matches(BLACK_REGEX, request.uri())) {
            return addBlackRequest(request);
        }

        int id = getId(request.uri(), false);
        if (id > 0) {
            try {
                return new DefaultResponseHtml().getHtml(getCorrectResponseInfo(id));
            } catch (Exception e) {
                logger.error("get request content error {}", e);
                return "<span class='layui-badge layui-bg-orange'>this is error</span><pre class='layui-code'>" + e + "</pre>";
            }
        }
        return new DefaultRequestHtml().buildHtml();
    }

    private String addBlackRequest(FullHttpRequest request) {
        logger.info("add Black Request..........");
        Matcher matcher = blackPattern.matcher(request.uri());
        if (matcher.matches()) {
            int id = Integer.parseInt(matcher.group(1));
            logger.info("add Black Request ,id {}", id);
            ResponseInfo responseInfo = getCorrectResponseInfo(id);
            if (responseInfo == null || responseInfo.getFullHttpRequest() == null) {
                return "{\"status\":0}";
            }
            String hostTxt = responseInfo.getFullHttpRequest().headers().get(HttpHeaderNames.HOST);
            String host = HttpUtils.getHost(hostTxt);
            WebProxyConstant.blackDomains.add(".*" + host + ".*");
            WebProxyConstant.responseInfoMap.remove(id);
            WebProxyConstant.container.remove(responseInfo.getFullHttpRequest());
            ReferenceCountUtil.release(responseInfo.getFullHttpRequest());
            ReferenceCountUtil.release(responseInfo.getFullHttpResponse());
            return "{\"status\":1}";
        }
        return "{\"status\":0}";
    }


    private String deleteRequest(FullHttpRequest request) {
        Map<String, List<String>> paramters = HttpRequestUtils.getPostParamters(request);
        logger.info("paramters {}", paramters);
        if (paramters.containsKey("id[]")) {
            List<String> idList = paramters.get("id[]");
            for (String s : idList) {
                ReferenceCountUtil.release(WebProxyConstant.responseInfoMap.get(Integer.parseInt(s)));
                WebProxyConstant.responseInfoMap.remove(Integer.parseInt(s));
            }
        }
        return "{\"status\":1}";
    }

    public int getId(String url, boolean isRetry) {

        Matcher matcher;
        if (isRetry) {
            matcher = retryPattern.matcher(url);
        } else {
            matcher = pattern.matcher(url);
        }
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;

    }

    public ResponseInfo getCorrectResponseInfo(int id) {

        if (!WebProxyConstant.responseInfoMap.containsKey(id)) {
            return null;
        }
        ResponseInfo targetResponseInfo = WebProxyConstant.responseInfoMap.get(id);
        FullHttpRequest fullHttpRequest = targetResponseInfo.getFullHttpRequest();
        FullHttpResponse fullHttpResponse = targetResponseInfo.getFullHttpResponse();
        if (fullHttpResponse == null) {
            fullHttpResponse = WebProxyConstant.container.get(fullHttpRequest);
            targetResponseInfo.setFullHttpResponse(fullHttpResponse);
            WebProxyConstant.container.remove(fullHttpRequest);
        }
        return targetResponseInfo;
    }

    private String retryRequest(FullHttpRequest request) {
        int id = getId(request.uri(), true);
        ResponseInfo responseInfo = getCorrectResponseInfo(id);
        FullHttpRequest fullHttpRequest = responseInfo.getFullHttpRequest();
        String hostTxt = fullHttpRequest.headers().get(HttpHeaderNames.HOST);
        String host = HttpUtils.getHost(hostTxt);
        int port = HttpUtils.getPort(hostTxt, fullHttpRequest.uri());
        logger.info("Retry Request,id {} host {} port {}", id, host, port);
        ReferenceCountUtil.retain(fullHttpRequest);
        ReferenceCountUtil.retain(fullHttpRequest);

        //send request
        ClientRequestInfo clientRequestInfo = new ClientRequestInfo.Builder().host(host).port(port).https(port == 443).reserve(true).msg(fullHttpRequest).build();
        ProxyClient proxyClient = new ProxyClient();
        proxyClient.setClientRequestInfo(clientRequestInfo);
        proxyClient.connectNewRemoteServer();

        //save container
        return saveAndReturnHtml(fullHttpRequest);
    }

    public String saveAndReturnHtml(FullHttpRequest fullHttpRequest) {

        int incrementid = WebProxyConstant.atomicInteger.incrementAndGet();
        ResponseInfo newResponseInfo = new ResponseInfo.Builder().id(incrementid)
                .fullHttpRequest(fullHttpRequest).build();
        WebProxyConstant.responseInfoMap.put(incrementid, newResponseInfo);
        String url = fullHttpRequest.uri();
        if (url.startsWith("/")) {
            url = "https://" + fullHttpRequest.headers().get(HttpHeaderNames.HOST) + url;
        }
        return DefaultRequestHtml.foreachHtml.replaceAll("\\{id\\}", String.valueOf(incrementid))
                .replaceAll("\\{url\\}", url);
    }


}
