package com.jiangnanyouzi.nettyproxy.utils;

import com.jiangnanyouzi.nettyproxy.config.WebProxyConstant;
import com.jiangnanyouzi.nettyproxy.web.ResponseInfo;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jiangnan on 2018/7/11.
 */
public class ResponseUtil extends ResponseUtils {

    public static final String MULTI_REGEX = ".*/.*/request/(\\d+)";
    private static Pattern multiPattern = Pattern.compile(MULTI_REGEX);

    public static String getHttpResponseTxt(FullHttpResponse fullHttpResponse) throws IOException {

        String contentType = fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE);
        String charset = StringUtils.defaultString(fullHttpResponse.headers().get(HttpHeaderNames.ACCEPT_CHARSET), HttpUtil.getCharset(contentType).name());
        byte[] bytes = ByteBufUtil.getBytes(fullHttpResponse.content());
        if (HttpHeaderValues.GZIP.toString().equalsIgnoreCase(fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_ENCODING))) {
            return HttpUtils.convertGzipStreamToString(bytes, charset);
        }
        return IOUtils.toString(bytes, charset);
    }

    public static int getId(String url) {

        Matcher matcher = multiPattern.matcher(url);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }


    public static ResponseInfo getCorrectResponseInfo(int id) {

        if (!WebProxyConstant.responseInfoMap.containsKey(id)) {
            return null;
        }
        return WebProxyConstant.responseInfoMap.get(id);
    }

    public static String toUpperCase(String word) {

        StringBuilder stringBuilder = new StringBuilder();
        String words[] = word.split("-");
        for (int i = 0, length = words.length; i < length; i++) {

            stringBuilder.append(StringUtils.capitalize(words[i]));
            if (i != length - 1) {
                stringBuilder.append("-");
            }
        }
        return stringBuilder.toString();

    }

    public static String fixUrl(FullHttpRequest fullHttpRequest, boolean https) {
        String url = fullHttpRequest.uri();
        if (url.startsWith("/")) {
            url = (https ? "https://" : "http://") + fullHttpRequest.headers().get(HttpHeaderNames.HOST) + url;
        }
        return url;
    }

}
