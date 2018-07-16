package com.jiangnanyouzi.nettyproxy.client;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.listener.DomainRequestListener;
import io.netty.handler.codec.http.*;
import org.junit.Test;

/**
 * Created by jiangnan on 2018/7/16.
 */
public class ProxyClientTest {
    @Test
    public void connectNewRemoteServer() throws Exception {

        String uri = "http://gss0.bdstatic.com/5foIcy0a2gI2n2jgoY3K/static/fisp_static/news/js/jquery-1.8.3.min_a6ffa58.js";
        ProxyConstant.clientListenerList.add(new DomainRequestListener(".*"));
        FullHttpRequest fullHttpRequest =
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);

        HttpHeaders httpHeaders = new DefaultHttpHeaders()
                .add(HttpHeaderNames.HOST, "gss0.bdstatic.com");
        fullHttpRequest.headers().add(httpHeaders);

        ClientRequestInfo clientRequestInfo = new ClientRequestInfo.Builder()
                .host("gss0.bdstatic.com").port(80).msg(fullHttpRequest)
                .reserve(true).https(false).build();

        ProxyClient proxyClient = new ProxyClient();

        proxyClient.setClientRequestInfo(clientRequestInfo);
        proxyClient.connectNewRemoteServer(clientRequestInfo);

        Thread.sleep(5 * 1000);
    }

}