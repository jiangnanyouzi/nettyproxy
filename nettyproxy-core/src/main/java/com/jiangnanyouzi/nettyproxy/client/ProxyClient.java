package com.jiangnanyouzi.nettyproxy.client;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.handler.ClientHandle;
import com.jiangnanyouzi.nettyproxy.listener.ClientListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ProxyClient {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private ChannelFuture channelFuture;
    private ClientRequestInfo clientRequestInfo;
    private boolean alreadySendData = false;
    private final ConcurrentLinkedQueue<Object> queue = new ConcurrentLinkedQueue<>();

    public ProxyClient() {

    }

    public void toServer(ClientRequestInfo clientRequestInfo) {


        setClientRequestInfo(clientRequestInfo);

        beforeRequest();

        if (!beforeConnect()) {
            return;
        }

        connectNewRemoteServer();

    }

    public void connectNewRemoteServer() {

        logger.info("client to server,start...........");

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ProxyConstant.nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                //.handler(new LoggingHandler(LogLevel.DEBUG))
                //.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5 * 1000)//5s
                .handler(new ChannelInitializer() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        if (clientRequestInfo.isHttps()) {
                            ch.pipeline().addLast(ProxyConstant.sslContext.newHandler(ch.alloc(), clientRequestInfo.getHost(), clientRequestInfo.getPort()));
                        }
                        ch.pipeline().addLast(new HttpClientCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(ProxyConstant.LIMIT));
                        ch.pipeline().addLast(new ClientHandle(clientRequestInfo));
                    }
                });
        channelFuture = bootstrap.connect(clientRequestInfo.getHost(), clientRequestInfo.getPort());
        channelFuture.addListener(new RequestChannelFutureListener());

    }

    private boolean beforeConnect() {

        if (channelFuture != null && !channelFuture.channel().isOpen()) {
            channelFuture.channel().close();
            channelFuture = null;
            return true;
        }

        if (channelFuture != null) {
            if (!alreadySendData || !channelFuture.channel().isOpen()) {
                queue.add(clientRequestInfo.getMsg());
                return false;
            }
            channelFuture.channel().writeAndFlush(clientRequestInfo.getMsg());
            return false;
        }
        return true;
    }

    public ChannelFuture getChannelFuture() {
        return channelFuture;
    }

    public ClientRequestInfo getClientRequestInfo() {
        return clientRequestInfo;
    }

    public void beforeRequest() {

        for (ClientListener clientListener : ProxyConstant.clientListenerList) {
            if (clientListener.shouldReserved(clientRequestInfo)) {
                clientRequestInfo.setReserve(true);
            }
        }

    }

    public void setClientRequestInfo(ClientRequestInfo clientRequestInfo) {
        this.clientRequestInfo = clientRequestInfo;
    }


    class RequestChannelFutureListener implements ChannelFutureListener {

        @Override

        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                future.channel().writeAndFlush(clientRequestInfo.getMsg());
                synchronized (queue) {
                    for (Object o : queue) {
                        future.channel().writeAndFlush(o);
                    }
                    queue.clear();
                    alreadySendData = true;
                }
                return;
            }
            logger.error("connect fail host {} port {}", clientRequestInfo.getHost(), clientRequestInfo.getPort());
            for (Object o : queue) {
                ReferenceCountUtil.release(o);
            }
            queue.clear();
            ReferenceCountUtil.release(clientRequestInfo.getMsg());
            clientRequestInfo.getChannelHandlerContext().channel().close();
            future.channel().close();
        }

    }
}
