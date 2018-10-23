package com.jiangnanyouzi.nettyproxy.main;


import com.jiangnanyouzi.nettyproxy.listener.DomainRequestListener;
import com.jiangnanyouzi.nettyproxy.server.ProxyServer;
import io.netty.util.ResourceLeakDetector;

/**
 * main
 *
 * @author
 * @create 2018-03-02 18:14
 **/
public class ServerMain {

    public static void main(String[] args) throws Exception {

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        ProxyServer.create().clientListener(new DomainRequestListener(".*baidu.com.*")).start();

    }

}
