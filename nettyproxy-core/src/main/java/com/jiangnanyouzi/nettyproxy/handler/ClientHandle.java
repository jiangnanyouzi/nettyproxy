package com.jiangnanyouzi.nettyproxy.handler;

import com.jiangnanyouzi.nettyproxy.client.ClientRequestInfo;
import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.listener.ClientListener;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientHandle extends ChannelInboundHandlerAdapter {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private ClientRequestInfo clientRequestInfo;

    public ClientHandle(ClientRequestInfo clientRequestInfo) {

        this.clientRequestInfo = clientRequestInfo;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("remoteAddress {} error {}", ctx.channel().remoteAddress(), cause.fillInStackTrace());
        ctx.close();
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        if (msg instanceof FullHttpResponse && clientRequestInfo.isReserve()) {
            afterResponse((FullHttpResponse) msg);
            determineRelease(this.clientRequestInfo.getMsg());
        }

        ChannelHandlerContext source = this.clientRequestInfo.getChannelHandlerContext();

        if (source == null || !source.channel().isOpen()) {
            if (source != null) {
                source.channel().close();
            }
            determineRelease(this.clientRequestInfo.getMsg());
            determineRelease(msg);
            return;
        }
        source.channel().writeAndFlush(msg).addListener(ChannelFutureListener.CLOSE);
    }

    private void afterResponse(FullHttpResponse response) {
        for (ClientListener clientListener : ProxyConstant.clientListenerList) {
            clientListener.process(clientRequestInfo, response);
        }
    }

    private void determineRelease(Object msg) {
        if (ReferenceCountUtil.refCnt(msg) > 0) {
            ReferenceCountUtil.safeRelease(msg);
        }
    }

}
