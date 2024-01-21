package com.hs.reactive.reactivestream.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * 自定义监听器
 */
public class MyListen {

    FluxSink<Object> sink;

    public MyListen(FluxSink<Object> sink) {
        this.sink = sink;
    }

    public void onClick(Object name){
        System.out.println("点击了"+name+"按钮");
        sink.next(name);
    }
}
