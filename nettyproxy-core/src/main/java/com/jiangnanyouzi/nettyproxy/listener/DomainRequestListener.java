package com.jiangnanyouzi.nettyproxy.listener;

import com.alibaba.fastjson.JSON;
import com.jiangnanyouzi.nettyproxy.utils.HttpRequestUtils;
import com.jiangnanyouzi.nettyproxy.utils.HttpUtils;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Created by jiangnan on 2018/7/2.
 */
public class DomainRequestListener extends AbstractClientListener {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private String[] domains;


    public DomainRequestListener() {
        this(".*");
    }

    public DomainRequestListener(String... domains) {
        this.domains = domains;
    }

    @Override
    public boolean shouldReservedHttpRequest(HttpRequest httpRequest) {
        for (String domain : domains) {
            if (Pattern.matches(domain, httpRequest.headers().get(HttpHeaderNames.HOST))) {
                ReferenceCountUtil.retain(httpRequest);
                return true;
            }
        }
        return false;
    }

    @Override
    public void process(HttpRequest httpRequest, FullHttpResponse fullHttpResponse) {

        logger.info("request path {}", HttpRequestUtils.getPath(httpRequest.uri()));
        if (httpRequest.method() == HttpMethod.POST) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) httpRequest;
            fullHttpRequest.content().resetReaderIndex();
            logger.info("POST paramters {}", JSON.toJSONString(HttpRequestUtils.getPostParamters(httpRequest)));
        }

        if (httpRequest.method() == HttpMethod.GET) {
            logger.info("GET paramters {}", JSON.toJSONString(HttpRequestUtils.getRequestParameters(httpRequest.uri())));
        }


        String contentType = fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_TYPE);

        if (StringUtils.isBlank(contentType)) {
            logger.info("contentType is blank");
            return;
        }

        String mimeType = HttpUtil.getMimeType(contentType).toString();
        logger.info("contentType {} MimeType {}", contentType, mimeType);
        logger.info("CONTENT_LENGTH {}", fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_LENGTH));
        logger.info("ACCEPT_CHARSET {}", fullHttpResponse.headers().get(HttpHeaderNames.ACCEPT_CHARSET));
        logger.info("CONTENT_ENCODING {}", fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_ENCODING));

        if (mimeType.contains("image")) {
            return;
        }

        String charset = StringUtils.defaultString(fullHttpResponse.headers().get(HttpHeaderNames.ACCEPT_CHARSET), HttpUtil.getCharset(contentType).name());
        byte[] bytes = ByteBufUtil.getBytes(fullHttpResponse.content());

        logger.info("charset {} ACCEPT {} ", charset, fullHttpResponse.headers().get(HttpHeaderNames.ACCEPT));

        if (HttpHeaderValues.GZIP.toString().equalsIgnoreCase(fullHttpResponse.headers().get(HttpHeaderNames.CONTENT_ENCODING))) {
            try {
                logger.info("content \n{}", HttpUtils.convertGzipStreamToString(bytes, charset));
            } catch (IOException ignored) {

            }
            return;
        }

        try {
            logger.info("content \n{}", IOUtils.toString(bytes, charset));
        } catch (IOException ignored) {

        }
    }
}
