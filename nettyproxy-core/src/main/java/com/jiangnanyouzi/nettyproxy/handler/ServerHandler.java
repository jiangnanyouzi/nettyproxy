package com.jiangnanyouzi.nettyproxy.handler;

import com.jiangnanyouzi.nettyproxy.client.ClientRequestInfo;
import com.jiangnanyouzi.nettyproxy.client.ProxyClient;
import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.utils.CertUtils;
import com.jiangnanyouzi.nettyproxy.utils.HttpUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

/**
 * HttpServerHandler
 *
 * @author
 * @create 2018-03-02 18:42
 **/
public class ServerHandler extends ChannelInboundHandlerAdapter {

    private final static HttpResponseStatus SUCCESS = new HttpResponseStatus(200, "Connection established");
    private Logger logger = LoggerFactory.getLogger(getClass());
    private String host;
    private int port;
    private KeyPair keyPair;
    private X509Certificate sources;
    private ProxyClient proxyClient = new ProxyClient();
    private HttpObjectAggregator aggr = new HttpObjectAggregator(ProxyConstant.LIMIT, true);
    private EmbeddedChannel embedder = new EmbeddedChannel(aggr);


    public ServerHandler(X509Certificate sources, KeyPair keyPair) {

        this.sources = sources;
        this.keyPair = keyPair;
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (proxyClient.getChannelFuture() != null) {
            proxyClient.getChannelFuture().channel().close();
        }
        ctx.channel().close();
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("remoteAddress {} error {}", ctx.channel().remoteAddress(), cause.fillInStackTrace());
        if (proxyClient.getChannelFuture() != null) {
            proxyClient.getChannelFuture().channel().close();
        }
        ctx.close();
    }


    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("remoteAddress {}", ctx.channel().remoteAddress());
    }


    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        logger.debug("================================================================");
        logger.debug("msg instanceof FullHttpRequest {}", msg instanceof FullHttpRequest);
        logger.debug("msg instanceof HttpRequest {}", msg instanceof HttpRequest);
        logger.debug("msg instanceof HttpContent {}", msg instanceof HttpContent);
        logger.debug("msg instanceof LastHttpContent {}", msg instanceof LastHttpContent);
        logger.debug("================================================================");

        if (msg instanceof FullHttpRequest) {

            FullHttpRequest request = (FullHttpRequest) msg;

            if (request.decoderResult().isFailure()) {
                logger.debug("Request Decode Fail,msg{}", msg);
                ctx.close();
                return;
            }

            String hostTxt = request.headers().get(HttpHeaderNames.HOST);

            this.host = HttpUtils.getHost(hostTxt);
            this.port = HttpUtils.getPort(hostTxt, request.uri());

            logger.info("HttpMethod {} Host {} port {}", request.method(), host, port);

            if (HttpMethod.CONNECT == request.method()) {
                sendOKResponse(ctx);
                ReferenceCountUtil.release(msg);
                return;
            }


            proxyClient.toServer(new ClientRequestInfo.Builder().channelHandlerContext(ctx).host(host).port(port).https(false).msg(msg).build());

            return;
        }

        if (msg instanceof ByteBuf && ((ByteBuf) msg).getByte(0) == 22) {
            sslHandShake(ctx, msg);
            return;
        }

        if (!(msg instanceof HttpRequest) && !(msg instanceof HttpContent)) {
            ReferenceCountUtil.release(msg);
            return;
        }

        //msg instanceof HttpRequest or HttpContent
        embedder.writeInbound(msg);
        if (msg instanceof LastHttpContent) {
            embedder.finish();
            FullHttpRequest aggratedMessage = embedder.readInbound();
            embedder.close();
            logger.info("finish {} , contentLength {}", embedder.finish(), HttpUtil.getContentLength(aggratedMessage));
            logger.info("Host {} port {}", host, port);
            proxyClient.toServer(new ClientRequestInfo.Builder().channelHandlerContext(ctx).host(host).port(port).https(true).msg(aggratedMessage).build());
        }

        //logger.info("Host {} port {}", host, port);
        //proxyClient.toServer(new ClientRequestInfo.Builder().channelHandlerContext(ctx).host(host).port(port).https(true).msg(msg).build());
    }

    private void sendOKResponse(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, SUCCESS);
        ctx.writeAndFlush(response);
        ctx.pipeline().remove("httpServerCodec");
        ctx.pipeline().remove("httpObject");

    }

    private void sslHandShake(ChannelHandlerContext ctx, Object msg) throws SSLException {

        logger.debug("===========================");
        logger.debug("SSL Hand Shake,start.......");
        logger.debug("===========================");

        X509Certificate x509Certificate = CertUtils.genCert(sources, keyPair.getPublic(), this.host);

        logger.debug("cert \n{}", CertUtils.convertToString(sources));
        logger.debug("cert \n{}", CertUtils.convertToString(x509Certificate));

        SslContext sslCtx = SslContextBuilder.forServer(keyPair.getPrivate(), x509Certificate).build();
        ctx.pipeline().addFirst("httpCodec", new HttpServerCodec());
        ctx.pipeline().addFirst("httpObject", new HttpObjectAggregator(ProxyConstant.LIMIT));
        ctx.pipeline().addFirst("sslHandle", sslCtx.newHandler(ctx.alloc()));
        ctx.pipeline().fireChannelRead(msg);

    }


}
