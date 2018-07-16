package com.jiangnanyouzi.nettyproxy.config;


import com.jiangnanyouzi.nettyproxy.listener.ClientListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class ProxyConstant {

    static {
        try {
            sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String CA_C = "CN";

    public static String CA_ST = "GD";

    public static String CA_L = "SZ";

    public static String CA_O = "io";

    public static String CA_OU = "netty";

    public static String CA_CN = "proxy";

    public static String CA_SHA = "SHA256WithRSAEncryption";

    public static URL CERT_FILE = ProxyConstant.class.getClassLoader().getResource("cert.crt");

    public static URL PRIVATEKEY_FILE = ProxyConstant.class.getClassLoader().getResource("private.der");

    public static Integer LIMIT = Integer.MAX_VALUE;

    public static List<ClientListener> clientListenerList = new ArrayList<>();

    public static SslContext sslContext;

    public static NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();

    public static int PORT = 9999;

}