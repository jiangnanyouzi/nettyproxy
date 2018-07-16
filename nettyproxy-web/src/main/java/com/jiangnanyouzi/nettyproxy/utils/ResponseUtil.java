package com.jiangnanyouzi.nettyproxy.utils;

import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * Created by jiangnan on 2018/7/11.
 */
public class ResponseUtil extends ResponseUtils {

    public static String getHttpResponseTxt(FullHttpResponse fullHttpResponse) throws IOException {

        String contentType = fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE);
        String charset = StringUtils.defaultString(fullHttpResponse.headers().get(HttpHeaderNames.ACCEPT_CHARSET), HttpUtil.getCharset(contentType).name());
        byte[] bytes = ByteBufUtil.getBytes(fullHttpResponse.content());
        if (HttpHeaderValues.GZIP.toString().equalsIgnoreCase(fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_ENCODING))) {
            return HttpUtils.convertGzipStreamToString(bytes, charset);
        }
        return IOUtils.toString(bytes, charset);
    }

}
