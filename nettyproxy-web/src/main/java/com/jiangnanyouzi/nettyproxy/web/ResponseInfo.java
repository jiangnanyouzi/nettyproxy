package com.jiangnanyouzi.nettyproxy.web;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

/**
 * Created by jiangnan on 2018/7/9.
 */
public class ResponseInfo {

    private int id;
    private FullHttpRequest fullHttpRequest;
    private FullHttpResponse fullHttpResponse;
    private Object request;
    private Object response;
    private boolean https;

    public ResponseInfo() {

    }

    private ResponseInfo(Builder builder) {
        setId(builder.id);
        setFullHttpRequest(builder.fullHttpRequest);
        setFullHttpResponse(builder.fullHttpResponse);
        setRequest(builder.request);
        setResponse(builder.response);
        setHttps(builder.https);
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public FullHttpRequest getFullHttpRequest() {
        return fullHttpRequest;
    }

    public void setFullHttpRequest(FullHttpRequest fullHttpRequest) {
        this.fullHttpRequest = fullHttpRequest;
    }

    public FullHttpResponse getFullHttpResponse() {
        return fullHttpResponse;
    }

    public void setFullHttpResponse(FullHttpResponse fullHttpResponse) {
        this.fullHttpResponse = fullHttpResponse;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public Object getResponse() {
        return response;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public boolean isHttps() {
        return https;
    }

    public void setHttps(boolean https) {
        this.https = https;
    }

    public static final class Builder {
        private int id;
        private FullHttpRequest fullHttpRequest;
        private FullHttpResponse fullHttpResponse;
        private Object request;
        private Object response;
        private boolean https;

        public Builder() {
        }

        public Builder id(int val) {
            id = val;
            return this;
        }

        public Builder fullHttpRequest(FullHttpRequest val) {
            fullHttpRequest = val;
            return this;
        }

        public Builder fullHttpResponse(FullHttpResponse val) {
            fullHttpResponse = val;
            return this;
        }

        public Builder request(Object val) {
            request = val;
            return this;
        }

        public Builder response(Object val) {
            response = val;
            return this;
        }

        public Builder https(boolean val) {
            https = val;
            return this;
        }

        public ResponseInfo build() {
            return new ResponseInfo(this);
        }
    }
}
