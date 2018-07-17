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
import java.util.HashMap;
import java.util.Map;
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


    public boolean shouldReservedHttpRequest(ClientRequestInfo requestInfo) {

        String host = ((FullHttpRequest) requestInfo.getMsg()).headers().get(HttpHeaderNames.HOST);
        for (String domain : WebProxyConstant.blackDomains) {
            if (Pattern.matches(domain, host)) {
                logger.info("{} is black", host);
                return false;
            }
        }

        for (String domain : domains) {
            logger.info("{} {}", host, Pattern.matches(domain, host));
            if (Pattern.matches(domain, host)) {
                int id = WebProxyConstant.atomicInteger.incrementAndGet();
                ResponseInfo responseInfo = new ResponseInfo.Builder().id(id).https(requestInfo.isHttps())
                        .fullHttpRequest((FullHttpRequest) requestInfo.getMsg()).build();
                WebProxyConstant.responseInfoMap.put(id, responseInfo);
                Map<String, Object> extras = new HashMap<>();
                extras.put(String.valueOf(id), responseInfo);
                requestInfo.setExtras(extras);
                ReferenceCountUtil.retain(requestInfo.getMsg());
                ReferenceCountUtil.retain(requestInfo.getMsg());
                return true;
            }
        }

        return false;
    }


    @Override
    public boolean shouldReserved(ClientRequestInfo requestInfo) {


        return checkLocalRequest(requestInfo);

    }

    @Override
    public void process(ClientRequestInfo requestInfo, FullHttpResponse fullHttpResponse) {

        for (String s : requestInfo.getExtras().keySet()) {
            WebProxyConstant.responseInfoMap
                    .get(Integer.valueOf(s))
                    .setFullHttpResponse(fullHttpResponse);
        }
        ReferenceCountUtil.retain(fullHttpResponse);

    }


    private boolean checkLocalRequest(ClientRequestInfo requestInfo) {

        if (("localhost".equalsIgnoreCase(requestInfo.getHost()) ||
                "127.0.0.1".equals(requestInfo.getHost())) &&
                requestInfo.getPort() == ProxyConstant.PORT) {
            String html = new ResponseHtml().buildHtml((FullHttpRequest) requestInfo.getMsg());
            HttpContent httpContent = new DefaultLastHttpContent();
            httpContent.content().writeBytes(html.getBytes());
            ChannelHandlerContext ctx = requestInfo.getChannelHandlerContext();
            ctx.writeAndFlush(ResponseUtils.getHtmlHttpResponse(html));
            ctx.writeAndFlush(httpContent);
            ctx.close();
            return false;
        }

        return shouldReservedHttpRequest(requestInfo);
    }

}
