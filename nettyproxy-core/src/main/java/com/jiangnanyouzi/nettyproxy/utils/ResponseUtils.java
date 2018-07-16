package com.jiangnanyouzi.nettyproxy.utils;

import io.netty.handler.codec.http.*;

/**
 * @author
 * @create 2018-04-01 23:31
 **/
public class ResponseUtils {


    public static HttpResponse getJsonHttpResponse(String json) {

        HttpHeaders httpHeaders = new DefaultHttpHeaders()
                .add(HttpHeaderNames.CONTENT_TYPE, "application/json;charset=utf-8")
                .add(HttpHeaderNames.CONTENT_LENGTH, json.getBytes().length);

        return getHttpResponse(httpHeaders, HttpResponseStatus.OK);
    }

    public static HttpResponse getHtmlHttpResponse(String html) {

        HttpHeaders httpHeaders = new DefaultHttpHeaders()
                .add(HttpHeaderNames.CONTENT_TYPE, "text/html;charset=utf-8")
                .add(HttpHeaderNames.CONTENT_LENGTH, html.getBytes().length);

        return getHttpResponse(httpHeaders, HttpResponseStatus.OK);
    }

    public static HttpResponse getHttpResponse(HttpHeaders httpHeaders, HttpResponseStatus status) {

        HttpResponse httpResponse = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        httpResponse.headers().add(httpHeaders);

        return httpResponse;
    }


}
