package com.jiangnanyouzi.nettyproxy.listener;

import com.jiangnanyouzi.nettyproxy.client.ClientRequestInfo;
import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.config.WebProxyConstant;
import com.jiangnanyouzi.nettyproxy.utils.ResponseUtils;
import com.jiangnanyouzi.nettyproxy.web.ResponseHtml;
import com.jiangnanyouzi.nettyproxy.web.ResponseInfo;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.regex.Pattern;


/**
 * Created by jiangnan on 2018/7/2.
 */
public class WebRequestListener implements ClientListener {

    private Logger logger = LoggerFactory.getLogger(getClass());
    public String[] domains;


    public WebRequestListener() {
        this(".*");
    }

    public WebRequestListener(String... domains) {

        this(domains, new String[]{});
    }

    public WebRequestListener(String[] domains, String[] blackDomains) {

        this.domains = domains;
        Collections.addAll(WebProxyConstant.blackDomains, blackDomains);
    }


    public boolean shouldReservedHttpRequest(HttpRequest httpRequest) {

        for (String domain : WebProxyConstant.blackDomains) {
            if (Pattern.matches(domain, httpRequest.headers().get(HttpHeaderNames.HOST))) {
                logger.info("{} is black", httpRequest.headers().get(HttpHeaderNames.HOST));
                return false;
            }
        }

        for (String domain : domains) {
            logger.info("{} {}", httpRequest.headers().get(HttpHeaderNames.HOST), Pattern.matches(domain, httpRequest.headers().get(HttpHeaderNames.HOST)));
            if (Pattern.matches(domain, httpRequest.headers().get(HttpHeaderNames.HOST))) {
                int id = WebProxyConstant.atomicInteger.incrementAndGet();
                ResponseInfo responseInfo = new ResponseInfo.Builder().id(id)
                        .fullHttpRequest((FullHttpRequest) httpRequest).build();
                WebProxyConstant.responseInfoMap.put(id, responseInfo);
                ReferenceCountUtil.retain(httpRequest);
                return true;
            }
        }

        return false;
    }


    public void processHttpResponse(HttpRequest httpRequest, FullHttpResponse fullHttpResponse) {

        WebProxyConstant.container.put((FullHttpRequest) httpRequest, fullHttpResponse);
        ReferenceCountUtil.retain(fullHttpResponse);
        ReferenceCountUtil.retain(httpRequest);

    }

    @Override
    public boolean shouldReserved(ClientRequestInfo requestInfo) {


        return checkLocalRequest(requestInfo.getHost(), requestInfo.getPort(), (FullHttpRequest) requestInfo.getMsg(), requestInfo.getChannelHandlerContext());

    }

    @Override
    public void process(ClientRequestInfo requestInfo, FullHttpResponse fullHttpResponse) {

        processHttpResponse((FullHttpRequest) requestInfo.getMsg(), fullHttpResponse);
    }


    private boolean checkLocalRequest(String host, int port, FullHttpRequest request, ChannelHandlerContext ctx) {

        if (("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) && port == ProxyConstant.PORT) {
            String html = new ResponseHtml().buildHtml(request);
            HttpContent httpContent = new DefaultLastHttpContent();
            httpContent.content().writeBytes(html.getBytes());
            ctx.writeAndFlush(ResponseUtils.getHtmlHttpResponse(html));
            ctx.writeAndFlush(httpContent);
            ctx.close();
            return false;
        }

        return shouldReservedHttpRequest(request);
    }

}
