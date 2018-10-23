package com.jiangnanyouzi.nettyproxy.web;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.config.WebProxyConstant;
import com.jiangnanyouzi.nettyproxy.utils.ResponseUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
            URL url = DefaultRequestHtml.class.getClassLoader().getResource("index.html");
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
            content = content.replaceAll("\\{foreach\\}(?s)(.*)\\{\\/foreach\\}", Matcher.quoteReplacement(handleForeach()));
            content = content.replaceAll("\\{port\\}", String.valueOf(ProxyConstant.PORT));
            content = content.replaceAll("\\{onSave\\}", String.valueOf(WebProxyConstant.onSave));
            content = content.replaceAll("\\{domains\\}", StringUtils.join(WebProxyConstant.domains, ","));
        } catch (Exception e) {
            logger.error("get default request content error {}", e);
            return "<span class='layui-badge layui-bg-orange'>this is error</span><pre class='layui-code'>" + ExceptionUtils.getStackTrace(e) + "</pre>";
        }
        return content;
    }

    public String handleForeach() {

        StringBuilder stringBuilder = new StringBuilder();
        for (Integer key : WebProxyConstant.responseInfoMap.keySet()) {
            ResponseInfo responseInfo = WebProxyConstant.responseInfoMap.get(key);
            stringBuilder.append(responseInfoConvertToHtml(responseInfo));
        }
        return stringBuilder.toString();
    }

    public String responseInfoConvertToHtml(ResponseInfo responseInfo) {
        String newHtml = foreachHtml.replaceAll("\\{id\\}", String.valueOf(responseInfo.getId()));
        newHtml = newHtml.replaceAll("\\{path\\}", Matcher.quoteReplacement(responseInfo.getFullHttpRequest().uri()));
        newHtml = newHtml.replaceAll("\\{httpMethod\\}", responseInfo.getFullHttpRequest().method().toString());
        newHtml = newHtml.replaceAll("\\{host\\}", responseInfo.getFullHttpRequest().headers().get("Host"));
        String statusCode = responseInfo.getFullHttpResponse() == null ? "-" : String.valueOf(responseInfo.getFullHttpResponse().status().code());
        newHtml = newHtml.replaceAll("\\{statusCode\\}", statusCode);
        return newHtml;
    }
}