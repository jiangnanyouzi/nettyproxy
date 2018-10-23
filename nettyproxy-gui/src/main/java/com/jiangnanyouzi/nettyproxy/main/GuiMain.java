package com.jiangnanyouzi.nettyproxy.main;

import com.jiangnanyouzi.nettyproxy.config.ProxyConstant;
import com.jiangnanyouzi.nettyproxy.listener.WebRequestListener;
import com.jiangnanyouzi.nettyproxy.server.ProxyServer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class GuiMain extends Application {

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webEngine.load("http://127.0.0.1:" + ProxyConstant.PORT);
        root.getChildren().add(webView);
        Scene scene = new Scene(root);
        primaryStage.setTitle("Netty Proxy Web");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ProxyServer.create().clientListener(new WebRequestListener()).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();


        Thread.sleep(5000);

        launch(args);
    }


}