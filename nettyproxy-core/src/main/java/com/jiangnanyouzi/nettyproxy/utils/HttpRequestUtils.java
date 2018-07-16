package com.jiangnanyouzi.nettyproxy.utils;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostMultipartRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author
 * @create 2018-04-01 22:22
 **/
public abstract class HttpRequestUtils {

    private static Logger logger = LoggerFactory.getLogger(HttpRequestUtils.class);

    public static String getPath(String url) {
        return new QueryStringDecoder(url).path();
    }

    public static Map<String, List<String>> getRequestParameters(String url) {
        return new QueryStringDecoder(url).parameters();
    }

    public static Map<String, List<String>> getPostParamters(HttpRequest request) {

        Map<String, List<String>> paramters = new HashMap<>();

        List<InterfaceHttpData> postList = new ArrayList<>();
        if (HttpPostRequestDecoder.isMultipart(request)) {
            HttpPostMultipartRequestDecoder multipartRequestDecoder = new HttpPostMultipartRequestDecoder(request);
            postList = multipartRequestDecoder.getBodyHttpDatas();
        } else if (request instanceof FullHttpRequest) {
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
            postList = decoder.getBodyHttpDatas();
        }
        for (InterfaceHttpData data : postList) {
            if (InterfaceHttpData.HttpDataType.Attribute == data.getHttpDataType()) {
                Attribute attribute = (Attribute) data;
                try {
                    if (paramters.containsKey(attribute.getName())) {
                        paramters.get(attribute.getName()).add(attribute.getValue());
                    } else {
                        List<String> valueList = new ArrayList<>();
                        valueList.add(attribute.getValue());
                        paramters.put(attribute.getName(), valueList);
                    }
                } catch (IOException e) {
                    logger.error("transform error {}", e);
                } finally {
                    data.release();
                }
            }
        }

        return paramters;
    }
}