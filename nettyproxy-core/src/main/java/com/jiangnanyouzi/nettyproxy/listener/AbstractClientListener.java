package com.jiangnanyouzi.nettyproxy.listener;

import com.jiangnanyouzi.nettyproxy.client.ClientRequestInfo;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Created by jiangnan on 2018/7/2.
 */
public abstract class AbstractClientListener implements ClientListener {

    @Override
    public boolean shouldReserved(ClientRequestInfo clientRequestInfo) {
        return clientRequestInfo.getMsg() instanceof HttpRequest && shouldReservedHttpRequest((HttpRequest) clientRequestInfo.getMsg());
    }

    @Override
    public void process(ClientRequestInfo clientRequestInfo, FullHttpResponse fullHttpResponse) {
        if (!(clientRequestInfo.getMsg() instanceof HttpRequest)) {
            return;
        }
        process((HttpRequest) clientRequestInfo.getMsg(), fullHttpResponse);
    }


    public abstract boolean shouldReservedHttpRequest(HttpRequest httpRequest);


    public abstract void process(HttpRequest httpRequest, FullHttpResponse fullHttpResponse);
}
