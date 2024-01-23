package com.hs.reactive.reactivestream.webflux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServer;

import java.io.IOException;

/**
 * 自定义的 webflux 服务
 * 使用 netty 作为服务器
 */
public class WebFluxServer {

    public static void main(String[] args) throws IOException {
        //处理器
        HttpHandler httpHandler = ((request, response) -> {
            System.out.println("收到请求"+request.getURI());
            //通过 response 中的 factory 获取dataBuffer,并往Mono中写入数据、
            DataBufferFactory dataBufferFactory = response.bufferFactory();
            DataBuffer wrap = dataBufferFactory.wrap("this is my response".getBytes());
            //往 response 中写数据
            return response.writeWith(Mono.just(wrap));
        });

        //2.定义一个请求处理适配器,并将处理器传入
        ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

        //3.使用 netty 启动一个服务，监听地址和端口，并绑定处理器
        HttpServer.create()
                .host("localhost")
                .port(8080)
                .handle(adapter)
                .bindNow();

        //卡住主线程
        System.in.read();
    }

}
