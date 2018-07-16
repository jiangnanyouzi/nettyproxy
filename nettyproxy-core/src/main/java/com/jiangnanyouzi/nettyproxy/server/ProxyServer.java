package com.jiangnanyouzi.nettyproxy.server;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.handler.ServerHandler;
import com.jiangnanyouzi.nettyproxy.listener.ClientListener;
import com.jiangnanyouzi.nettyproxy.utils.CertUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
 * http proxy
 *
 * @author
 * @create 2018-03-02 18:13
 **/
public class ProxyServer {

    private  Logger logger = LoggerFactory.getLogger(getClass());


    public ProxyServer clientListener(ClientListener... clientListener) {
        if (clientListener.length > 0) {
            List<ClientListener> clientListenerList = ProxyConstant.clientListenerList;
            clientListenerList.addAll(Arrays.asList(clientListener));
        }
        return this;
    }

    public void start() throws Exception {
        start(9999);
    }

    public void start(int port) throws Exception {

        logger.info("server port {} , HttpProxyServer start.......", port);
        ProxyConstant.PORT = port;
        startProxy();
    }

    private void startProxy() throws Exception {

        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator caKeyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
        caKeyPairGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = caKeyPairGen.generateKeyPair();
        X509Certificate sources = CertUtils.load();

        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    //.childOption(ChannelOption.TCP_NODELAY, true)
                    //.childOption(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    .childHandler(new ChannelInitializer<Channel>() {

                        @Override
                        protected void initChannel(Channel ch) throws Exception {

                            ch.pipeline().addLast("httpServerCodec", new HttpServerCodec());
                            ch.pipeline().addLast("httpObject", new HttpObjectAggregator(ProxyConstant.LIMIT));
                            ch.pipeline().addLast("serverHandle", new ServerHandler(sources, keyPair));
                        }
                    });
            ChannelFuture f = b
                    .bind(ProxyConstant.PORT)
                    .sync();
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }


}
