package com.jiangnanyouzi.nettyproxy.listener;

import com.jiangnanyouzi.nettyproxy.client.ClientRequestInfo;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Created by jiangnan on 2018/7/2.
 */
public interface ClientListener {

    boolean shouldReserved(ClientRequestInfo clientRequestInfo);

    void process(ClientRequestInfo clientRequestInfo, FullHttpResponse fullHttpResponse);
}
