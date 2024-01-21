package com.hs.reactive.reactivestream.reactor;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class FluxApiTest {

    /**
     * 生成序列数据
     */
    public static void generateTest() {
        Flux<Object> generate = Flux.generate(
                        () -> 0,//定义一个初始值 state 为 0
                        (state, sink) -> {
                            //state 为初始值，sink为接收器，通道，用于传输数据
                            sink.next(state);
                            //我需要发送0-10的元素，那么在state = 10 时，我手动完成，发送完成信号
                            if (state == 10) sink.complete();
                            //或者报错等
//                    if(state == 8)sink.error(new RuntimeException("报错了"));
                            return state + 1;
                        })
                .delayElements(Duration.ofSeconds(2));
        Disposable subscribe = generate.log().subscribe();

        try {
            Thread.sleep(3000);
            //Disposable 中有一个方法，可以取消订阅
            //调用 dispose 方法后，会打印出是以  cancel() 结束的
            subscribe.dispose();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * create 方法测试
     * 生成序列数据，适用多线程环境
     */
    public static void createTest() {
        Flux.create(fluxSink -> {
            MyListen myListen = new MyListen(fluxSink);
            myListen.onClick("");
        });
    }

    /**
     * 自定义流元素的处理规则
     */
    public static void handleTest() {
        Flux.range(1, 10)
                .handle((value, sink) -> {
                    System.out.println("拿到了流里面的值" + value);
                    //修改值再通过 sink 传送下一步
                    sink.next(value + value);
                })
                .subscribe(System.out::println)
        ;
    }

    /**
     * 定义线程调度策略
     * .publishOn 改变发布者的线程
     * .subscribeOn 改变订阅者的线程
     */
    public static void threadTest() {
        Flux.range(1, 10)
                //可以看到 log 打印出来的线程名称发生了变化
                .publishOn(Schedulers.single())
                .log()
//                .subscribeOn()
                .subscribe()
        ;
        //有界的线程池，不会无限扩展的，默认是 10*CPU核心数；队列默认十万
//        Schedulers.boundedElastic();
        //用当前线程运行所有操作
//        Schedulers.immediate();
        //另外起一个线程，然后使用新的这个单线程进行执行
//        Schedulers.single();
        //自己定义一个线程池
//        Schedulers.fromExecutor(new ThreadPoolExecutor(1,1))
    }

    /**
     * transform 方法测试
     */
    public static void transformTest() {
        AtomicInteger integer = new AtomicInteger(0);
        Flux<String> transform = Flux.just("a", "b", "c")
                //transform：不会共享外部变量的值。无状态转换;原理，无论多少个订阅者，transform只执行一次
                //transformDeferred 会共享外部变量的值。有状态转换;原理，无论多少个订阅者，每个订阅者transform都执行一次

                .transformDeferred(v -> {
                    // //使用 transformDeferred 打印结果全部为第一次订阅为A,B,C。第二次订阅的打印为a,b,c
//                .transform(v -> {
                    //使用 transform 打印结果全部为大写的A,B,C
                    if (integer.incrementAndGet() == 1) {
                        //表示是第一次执行
                        //将值转为大写再返回
                        return v.map(String::toUpperCase);
                    } else {
                        //否则不处理直接返回
                        return v;
                    }
                });
        //订阅两次
        transform.subscribe(s -> System.out.println("第一个订阅：" + s));
        transform.subscribe(s -> System.out.println("第二个订阅：" + s));
    }

    private static Mono<String> getMono() {
//        return  Mono.just("ceshi");
        return Mono.empty();
    }

    /**
     * empty 方法测试
     */
    public static void emptyTest() {
        //注意，两个的区别为：
        // defaultIfEmpty 只能写定义好的静态数据
        // switchIfEmpty 可以调用方法获得一个新流
        getMono()
                //如果从 getMono 中没获取到元素，那么值默认为 xixi
                .defaultIfEmpty("xixi")
                .log()
                .subscribe();

        getMono()
                //如果从 getMono 中没获取到元素，那么会调用 switchIfEmpty 中的动态方法，返回一个新流
                .switchIfEmpty(Mono.just("我是swith的数据"))
                .log()
                .subscribe();
    }


    /**
     * merge 方法测试
     * merge 和 concat 的区别为：
     * concat是一个流一个流的合并数据，后面的流数据会追加到前面的流后面
     * 而 merge 则是根据流的元素到达时间，直接插入最后，所以可能会存在每个流的数据都被打乱分开了
     */
    public static void mergeTest() {
        // concat的话一定要等到一个流的数据全部拿到，才回去做 concat 操作
        Flux.concat(Flux.just("a", "b", "c").delayElements(Duration.ofSeconds(1)), Flux.range(1, 10))
                .subscribe(s -> System.out.println("这是concat的输出：" + s));

        //加上 delayElements 时间之后，可以很明显的看到第一个流的元素出现在了第二个的后面
        Flux.merge(Flux.just("a", "b", "c").delayElements(Duration.ofSeconds(1)), Flux.range(1, 10))
                .subscribe(s -> System.out.println("这是merge--》的输出：" + s));
    }

    /**
     * zip 方法测试
     */
    public static void zipTest() {
        //zip 会将你在调用方法的参数中穿的流合并到一起，最多支持传入8个流
        Flux.zip(Flux.just(4, 5, 6), Flux.just(4, 5, 6), Flux.just(4, 5, 6))
                .map(s -> {
                    System.out.println(s.getClass());
                    return s;
                })
                .log()
                .subscribe();

        // zipWith 只能在流后面调用，并且只能将两个流合并到一起
        Flux.just(1, 2, 3)
                //将两个流的元素，按下标的顺序，压缩到一个 tuple（元组） 中，如果对应下标中没有元素了，那么上层元素也会被忽略
                .zipWith(Flux.just(4, 5, 6))
                .map(s -> {
                    Integer t1 = s.getT1();
                    Integer t2 = s.getT2();
                    return t1 + t2;
                })
                .zipWith(Flux.just("haha", "oo", "kk"))
                .log()
                .subscribe();
    }


    public static void main(String[] args) throws IOException {
//        generateTest();
//        handleTest();
//        threadTest();
//        transformTest();
//        emptyTest();
//        mergeTest();
        zipTest();
        System.in.read();

    }
}
