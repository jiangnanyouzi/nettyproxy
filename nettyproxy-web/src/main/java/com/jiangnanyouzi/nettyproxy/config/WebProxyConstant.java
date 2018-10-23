package com.jiangnanyouzi.nettyproxy.config;


import com.jiangnanyouzi.nettyproxy.web.ResponseInfo;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class WebProxyConstant {


    public static AtomicInteger atomicInteger = new AtomicInteger(0);

    public static Map<Integer, ResponseInfo> responseInfoMap = new HashMap<>();

    public static Set<String> blackDomains = new TreeSet<>();

    public static boolean onSave = true;

    public static String[] domains = new String[]{".*"};

}