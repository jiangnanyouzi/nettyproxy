package com.jiangnanyouzi.nettyproxy.main;

import com.jiangnanyouzi.nettyproxy.listener.WebRequestListener;
import com.jiangnanyouzi.nettyproxy.server.ProxyServer;
import io.netty.util.ResourceLeakDetector;

/**
 * Created by jiangnan on 2018/7/11.
 */
public class Main {
    public static void main(String[] args) throws Exception {

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        ProxyServer.create().clientListener(new WebRequestListener()).start();
    }
}
