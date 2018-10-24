# nettyproxy
一个用netty捕获http https请求的工具,类似于fiddler.anyproxy.whistle
<pre>
    1.生成证书
    <blockqute>
        a.运行 CertificateUtils.saveX509Certificate();
        b.也可以自己定制,了解这个该方法即可.
        c.resources目录也有生成好的,可以直接安装
    </blockqute>
</pre>
<pre>
    2.安装证书(https用到的).请百度.firefox会比较特别的.
</pre>
<pre>
    3.运行 mian包下面ServerMain.main();
    4.浏览器连上代理即可 http 代理 默认端口 9999
    你想要看到的东西都会在日志打印出来(默认是打印百度).
</pre>
<pre>
    如果没有看到内容,请确认上面步骤没错
</pre>
<pre>
    nettyproxy-web,是一个扩展,把捕捉所有请求通过web页面显示.
    
        运行 main 方法即可;
        浏览器打开127.0.0.1:9999显示捕捉的请求.
        ps.页面很一般啊
        
</pre>
![](http://bbs1.people.com.cn/postImages/Y0/A3/E1/9F/E6/1540347764710.png)
<pre>
    代码量比较少,难免有bug.很多功能也没做.欢迎.............
</pre>
<pre>
    windows已打包,<a href="https://github.com/jiangnanyouzi/nettyproxy/releases" target="_blank">下载</a>
</pre>
