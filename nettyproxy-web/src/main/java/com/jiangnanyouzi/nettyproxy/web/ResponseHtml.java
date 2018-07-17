package com.jiangnanyouzi.nettyproxy.web;

import com.jiangnanyouzi.nettyproxy.client.ClientRequestInfo;
import com.jiangnanyouzi.nettyproxy.client.ProxyClient;
import com.jiangnanyouzi.nettyproxy.config.WebProxyConstant;
import com.jiangnanyouzi.nettyproxy.utils.HttpRequestUtils;
import com.jiangnanyouzi.nettyproxy.utils.HttpUtils;
import com.jiangnanyouzi.nettyproxy.utils.ResponseUtil;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.jiangnanyouzi.nettyproxy.web.RquestResolver.Edit_REGEX;
import static com.jiangnanyouzi.nettyproxy.web.RquestResolver.POST_REGEX;

/**
 * Created by jiangnan on 2018/7/9.
 */
public class ResponseHtml {

    public static final String RETRY_REGEX = ".*/retry/request/(\\d+)";
    public static final String BLACK_REGEX = ".*/black/request/(\\d+)";
    public static final String DELETE_REGEX = ".*/delete/request.*";

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

        if (Pattern.matches(Edit_REGEX, request.uri()) || Pattern.matches(POST_REGEX, request.uri())) {
            return new RquestResolver(request).resolve();
        }

        int id = ResponseUtil.getId(request.uri());
        if (id > 0) {
            try {
                return new DefaultResponseHtml().getHtml(ResponseUtil.getCorrectResponseInfo(id));
            } catch (Exception e) {
                logger.error("get request content error {}", e);
                return "<span class='layui-badge layui-bg-orange'>this is error</span><pre class='layui-code'>" + ExceptionUtils.getStackTrace(e) + "</pre>";
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
            ResponseInfo responseInfo = ResponseUtil.getCorrectResponseInfo(id);
            if (responseInfo == null || responseInfo.getFullHttpRequest() == null) {
                return "{\"status\":0}";
            }
            String hostTxt = responseInfo.getFullHttpRequest().headers().get(HttpHeaderNames.HOST);
            String host = HttpUtils.getHost(hostTxt);
            WebProxyConstant.blackDomains.add(".*" + host + ".*");
            WebProxyConstant.responseInfoMap.remove(id);
            releaseResponseInfo(responseInfo);
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
                ResponseInfo responseInfo = WebProxyConstant.responseInfoMap.get(Integer.parseInt(s));
                releaseResponseInfo(responseInfo);
                WebProxyConstant.responseInfoMap.remove(Integer.parseInt(s));
            }
        }
        return "{\"status\":1}";
    }


    private String retryRequest(FullHttpRequest request) {
        int id = ResponseUtil.getId(request.uri());
        ResponseInfo responseInfo = ResponseUtil.getCorrectResponseInfo(id);
        if (responseInfo == null) {
            return "error";
        }
        FullHttpRequest sources = responseInfo.getFullHttpRequest();
        String hostTxt = sources.headers().get(HttpHeaderNames.HOST);
        String host = HttpUtils.getHost(hostTxt);
        int port = responseInfo.isHttps() ? 443 : 80;

        logger.info("Retry Request,id {} host {} port {}", id, host, port);

        FullHttpRequest fullHttpRequest = sources.copy();
        //send request
        ClientRequestInfo.Builder builder = new ClientRequestInfo.Builder().host(host).port(port).reserve(true);
        ClientRequestInfo clientRequestInfo = builder.https(responseInfo.isHttps()).msg(fullHttpRequest.copy()).build();
        ProxyClient proxyClient = new ProxyClient();
        proxyClient.setClientRequestInfo(clientRequestInfo);
        proxyClient.connectNewRemoteServer();

        //save container
        return saveAndReturnHtml(fullHttpRequest, clientRequestInfo);
    }

    public String saveAndReturnHtml(FullHttpRequest fullHttpRequest, ClientRequestInfo clientRequestInfo) {

        int incrementid = WebProxyConstant.atomicInteger.incrementAndGet();
        ResponseInfo newResponseInfo = new ResponseInfo.Builder().id(incrementid).https(clientRequestInfo.isHttps())
                .fullHttpRequest(fullHttpRequest).build();
        Map<String, Object> extras = new HashMap<>();
        extras.put(String.valueOf(incrementid), newResponseInfo);
        clientRequestInfo.setExtras(extras);
        WebProxyConstant.responseInfoMap.put(incrementid, newResponseInfo);
        String url = ResponseUtil.fixUrl(fullHttpRequest, clientRequestInfo.isHttps());
        return DefaultRequestHtml.foreachHtml.replaceAll("\\{id\\}", String.valueOf(incrementid))
                .replaceAll("\\{url\\}", url);
    }

    private void releaseResponseInfo(ResponseInfo responseInfo) {
        FullHttpRequest fullHttpRequest = responseInfo.getFullHttpRequest();
        FullHttpResponse fullHttpResponse = responseInfo.getFullHttpResponse();
        while (ReferenceCountUtil.refCnt(fullHttpRequest) > 0) {
            ReferenceCountUtil.release(fullHttpRequest);
        }
        while (ReferenceCountUtil.refCnt(fullHttpResponse) > 0) {
            ReferenceCountUtil.release(fullHttpResponse);
        }
    }

}
