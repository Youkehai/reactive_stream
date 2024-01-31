package com.hs.reactive.reactivestream.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.io.IOException;
import java.time.Duration;
import java.util.PriorityQueue;

/**
 * 重试机制
 */
public class FluxRetryDeomo {

    /**
     * 重试方法
     */
    static void retryTest(){
        //模拟超时异常
        Flux.just(1)
                //模拟超时异常，三秒后再返回元素
                .delayElements(Duration.ofSeconds(3))
                .log()
                //一秒超时
                .timeout(Duration.ofSeconds(1))
                // retry(3) 方法会将流重试三次
                .retry(3)
                .log()
                .subscribe(System.out::println);
    }

    /**
     * Sinks: 接收器，数据管道，数据流顺着管道往下流转
     *
     */
    static void sinksTest(){
        //发送多个数据，相当于 flux
        // .unicast() 单播 ，该管道只能绑定一个消费者
//        Sinks.many().unicast();
//        // .multicast() 多播，可以绑定多个消费者
//        Sinks.many().multicast();
//        //多个消费者，是否要重放，相当于当前订阅者已经发到10了，但是此时第二个消费者才过来，需要重放的话，那么会将以前的数据按照后面的方法limit()或者all()发送给消费者2
//        // 如果不需要重放，那么继续从10开始消费
//        Sinks.many().replay();
//        //背压，相当于缓冲区最多放十个元素
//        Sinks.many().unicast().onBackpressureBuffer(new PriorityQueue<>(10));
//
//        //发送一个数据，相当于 Mono
//        Sinks.one();

        Flux<Integer> cache = Flux.range(1, 10)
                .delayElements(Duration.ofSeconds(1))
                //相当于缓存 2 个元素，当后面的订阅者过来时，会将以前缓存的这 2 个元素也发给他
                //不设置的话，默认缓存所有，如果设置为 0 ，那么只会缓存最终的信号
                .cache(3);
        cache.subscribe();
        new Thread(()->{
            //像我这样的写法，缓存了3个元素，然后五秒后才开始第二个消费，那么第二个订阅者就是从 5-3=2 的这个元素开始消费的
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            cache.subscribe(s-> System.out.println("我是第二个订阅者"+s));
        }).start();
    }

    /**
     * 阻塞式 api 测试
     */
    static void blockTest(){
        //阻塞式等结果，拿到第一个元素
        Integer integer = Flux.just(1, 2, 3, 4, 5, 6)
                .filter(i -> i > 3)
                .blockFirst();
        System.out.println(integer);
    }

    /**
     *  parallel 并发处理测试
     */
    static void parallelTest(){
        Flux.range(1,100)
                .buffer(10)//将100个元素分成10个为一个集合
                .parallel(10)//用十个线程并发处理
                .runOn(Schedulers.newParallel("my-parallel"))//用 my-parallel 开头的线程来跑
                .log()
                .subscribe();
    }

    /**
     * 响应式编程中的 threadLocal
     */
    static void contextTest(){
        Flux.just(1,2,3,4,5,6)
                .contextWrite(Context.of("myContext1","myValue1"))
                //方法名称中带 Contextual 的表示支持 context
                .transformDeferredContextual((item,context)->{
                    //这里会打印出下面 contextWrite 方法中写入的数据 {myContext=myValue}，而不会打印 {myContext1=myValue1}
                    System.out.println(context);
                    return Flux.range(1,9);
                })
                //一定要注意 contextWrite 只能从下游传播给上游，所以上游需要使用时，一定要定义在下面
                .contextWrite(Context.of("myContext","myValue"))
                .subscribe()
        ;
    }

    public static void main(String[] args) throws IOException {
//        retryTest();
//        sinksTest();
//        blockTest();
//        parallelTest();
        contextTest();
        System.in.read();

    }
}
