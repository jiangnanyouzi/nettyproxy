package com.jiangnanyouzi.nettyproxy.utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * Created by jiangnan on 2018/3/21.
 */
public class HttpUtils {

    private static Logger logger = LoggerFactory.getLogger(HttpUtils.class);


    public static String getHost(String hostTxt) {
        if (StringUtils.isBlank(hostTxt)) {
            logger.info("hostTxt {}", hostTxt);
            throw new IllegalArgumentException("host is illegal");
        }
        if (StringUtils.contains(hostTxt, ":")) {
            return hostTxt.split(":")[0];
        }
        return hostTxt;
    }

    public static Integer getPort(String hostTxt, String url) {

        if (StringUtils.isBlank(hostTxt)) {
            throw new IllegalArgumentException("host is illegal");
        }
        if (hostTxt.split(":").length > 1) {
            return NumberUtils.toInt(hostTxt.split(":")[1], 80);
        }
        if (url.split(":").length > 1) {
            return NumberUtils.toInt(url.split(":")[1], 80);
        }
        logger.info("url {}", url);
        return url.indexOf("https") == 0 ? 443 : 80;

    }

    public static byte[] unGzip(byte[] data) throws IOException {

        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        GZIPInputStream gzip = new GZIPInputStream(bis);
        byte[] ret = IOUtils.toByteArray(gzip);
        gzip.close();
        bis.close();
        return ret;
    }

    public static String convertGzipStreamToString(byte[] data, String charset) throws IOException {

        return IOUtils.toString(unGzip(data), charset);

    }

    public static String relativePath(String absolutePath) {

        StringBuilder stringBuilder = new StringBuilder();
        try {
            URL url = new URL(absolutePath);
            if (StringUtils.isNoneBlank(url.getPath())) {
                stringBuilder.append(url.getPath());
            }
            if (StringUtils.isNoneBlank(url.getQuery())) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("?");
                }
                stringBuilder.append(url.getQuery());
            }
            if (StringUtils.isNoneBlank(url.getRef())) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append("#");
                }
                stringBuilder.append(url.getRef());
            }
            if (stringBuilder.length() == 0) {
                stringBuilder.append("/");
            }
            return stringBuilder.toString();
        } catch (MalformedURLException e) {
            return absolutePath;
        }
    }



}
