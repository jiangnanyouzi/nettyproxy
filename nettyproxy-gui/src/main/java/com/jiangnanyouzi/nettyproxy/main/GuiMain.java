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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.Charset;

public class GuiMain extends Application {

    private static Logger logger = LoggerFactory.getLogger(GuiMain.class);

    static {
        try {
            System.setProperty("file.encoding", "UTF-8");
            Field charset = Charset.class.getDeclaredField("defaultCharset");
            charset.setAccessible(true);
            charset.set(null, null);
        } catch (Exception e) {
            logger.error("set charset error {}", e);
        }
    }

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
        new Thread(() -> {
            try {
                ProxyServer.create().clientListener(new WebRequestListener()).start();
            } catch (Exception e) {
                logger.error("proxy server not start {}", e);
                throw new RuntimeException(e);
            }
        }).start();


        Thread.sleep(5000);

        launch(args);
    }


}