package com.jiangnanyouzi.nettyproxy.web;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.config.WebProxyConstant;
import com.jiangnanyouzi.nettyproxy.utils.ResponseUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultRequestHtml {

    public static Pattern pattern = Pattern.compile("\\{foreach\\}(.*)\\{\\/foreach\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static Logger logger = LoggerFactory.getLogger(DefaultRequestHtml.class);

    public static String templateContent = null;
    public static String foreachHtml = null;

    static {
        try {
            URL url = DefaultRequestHtml.class.getClassLoader().getResource("template.html");
            if (url != null) {
                templateContent = IOUtils.toString(url, "UTF-8");
                Matcher match = pattern.matcher(templateContent);
                if (match.find()) {
                    foreachHtml = match.group(1);
                }
            }
        } catch (IOException e) {
            logger.error("error {}", e);
        }
    }

    public String buildHtml() {

        String content = templateContent;
        try {
            content = content.replaceAll("\\{foreach\\}(?s)(.*)\\{\\/foreach\\}", Matcher.quoteReplacement(handleForeach(foreachHtml)));
            content = content.replaceAll("\\{port\\}", String.valueOf(ProxyConstant.PORT));
        } catch (Exception e) {
            logger.error("get default request content error {}", e);
            return "<span class='layui-badge layui-bg-orange'>this is error</span><pre class='layui-code'>" + ExceptionUtils.getStackTrace(e) + "</pre>";
        }
        return content;
    }

    public String handleForeach(String foreachHtml) {

        StringBuilder stringBuilder = new StringBuilder();
        for (Integer key : WebProxyConstant.responseInfoMap.keySet()) {
            ResponseInfo responseInfo = WebProxyConstant.responseInfoMap.get(key);
            String url= ResponseUtil.fixUrl(responseInfo.getFullHttpRequest(),responseInfo.isHttps());
            String newHtml = foreachHtml.replaceAll("\\{id\\}", String.valueOf(responseInfo.getId()))
                    .replaceAll("\\{url\\}", Matcher.quoteReplacement(url));
            stringBuilder.append(newHtml);
        }
        return stringBuilder.toString();
    }
}