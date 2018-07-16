package com.jiangnanyouzi.nettyproxy.client;

import io.netty.channel.ChannelHandlerContext;

/**
 * Created by jiangnan on 2018/7/2.
 */
public class ClientRequestInfo {

    private ChannelHandlerContext channelHandlerContext;
    private String host;
    private int port;
    private Object msg;
    private boolean https;
    private boolean reserve = false;

    public ClientRequestInfo() {

    }

    private ClientRequestInfo(Builder builder) {
        setChannelHandlerContext(builder.channelHandlerContext);
        setHost(builder.host);
        setPort(builder.port);
        setMsg(builder.msg);
        setHttps(builder.https);
        setReserve(builder.reserve);
    }


    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Object getMsg() {
        return msg;
    }

    public void setMsg(Object msg) {
        this.msg = msg;
    }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }

    public boolean isReserve() {
        return reserve;
    }

    public void setReserve(boolean reserve) {
        this.reserve = reserve;
    }


    public static final class Builder {
        private ChannelHandlerContext channelHandlerContext;
        private String host;
        private int port;
        private Object msg;
        private boolean https;
        private boolean reserve;

        public Builder() {
        }

        public Builder channelHandlerContext(ChannelHandlerContext val) {
            channelHandlerContext = val;
            return this;
        }

        public Builder host(String val) {
            host = val;
            return this;
        }

        public Builder port(int val) {
            port = val;
            return this;
        }

        public Builder msg(Object val) {
            msg = val;
            return this;
        }

        public Builder https(boolean val) {
            https = val;
            return this;
        }

        public Builder reserve(boolean val) {
            reserve = val;
            return this;
        }

        public ClientRequestInfo build() {
            return new ClientRequestInfo(this);
        }
    }
}
