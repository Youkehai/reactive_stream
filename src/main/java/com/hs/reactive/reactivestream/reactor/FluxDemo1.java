package com.hs.reactive.reactivestream.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * 响应式编程中，主要包含
 * Mono 和 Flux
 * 其中 Mono 代表0|1个元素的流
 * Flux 代表多个元素的流
 * doOnxxx 方法，表示发生这个事件时，触发回调（不能改变元素和信号，只能做一些其他的处理）
 * onXxx   方法，表示发生这个事件后执行一个动作，可以改变元素或者信号，再继续传递
 */
public class FluxDemo1 {
    /**
     * flux 可以有多个元素
     *
     * @throws IOException
     */
    public static void flux() throws IOException {
        //1.定义一个多元素的流
        Flux<Integer> just = Flux.just(1, 2, 3);
        //订阅这个流 进行消费
        just.subscribe(System.out::println);
        //可以被多次消费，相当于是 mq 的广播模式
        just.subscribe(s -> System.out.println("第二个" + s));

        //2.定时生成一个元素
        //此处定义的为2秒生成一个元素
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(2));
        interval.subscribe(System.out::println);

    }

    /**
     * Mono 测试
     * mono 只能有一个元素
     *
     * @throws IOException
     */
    public static void mono() throws IOException {
        Mono<Integer> just = Mono.just(1);
        just.subscribe(System.out::println);
    }

    /**
     * 测试各种方法，主要测试 doOn 开头的方法
     */
    public static void methodTest() {
        //链式调用中，下边的方法消费的流，都是上一个方法操作完之后的流
        //doOn 开头的方法为钩子函数（Hook）,当流达到某个条件时，会自动触发该方法
        Flux<Integer> just = Flux.range(1, 10)
                .delayElements(Duration.ofSeconds(1))//整流（调整频率），让流延时定义好的时间再继续流转
//                .doOnError(throwable->{
//                    //如果将 doOnError 放在可能会发生错误的操作前面，那么则不会进来该回调
//                    //doOnError 的位置，要尽量放在最后，则它可以拿到（感知到）前面所有步骤的错误信息
//                    //当流发生错误时的会回调
//                    System.out.println("我是flux的报错了222"+throwable);
//                })

                .map(s -> {
                    if (s > 8) {
                        //在这里故意创造错误，让下面的 doOnError 能拿到我这个流处理造成的异常
                        //如果将 doOnNext 放到 .map的后面，那么 doOnNext 只能打印到 8，如果放在前面，则能打印到9
                        s = s / 0;
                    }
                    return s;
                })
                .onErrorComplete()//这个方法会在当前面的流发生异常时，也能正常结束流，而不是异常结束，会回调 doOnComplete 方法
                .doOnEach(item -> {
                    //任务东西来了，都会进来，包括元素和信号
                    System.out.println("任务东西来了，都会进来，包括元素和信号" + item);
                })
                .doOnNext(item -> {
                    //当流的下一个元素到达时的回调,只会有元素，不会有信号
                    System.out.println("元素来了" + item);
                })
                .filter(s -> s > 3)
                .doOnDiscard((Integer.class), s -> {
                    //当某个流被忽略时的回调,例如上面调用了 filter ，只要大于 3 的数据
                    System.out.println("元素被忽略了" + s);
                })
                .doOnRequest(s -> {
                    //当消费者请求数据时，触发该方法
                    System.out.println("消费者来请求数据了：" + s);
                })
                .doOnComplete(() -> {
                    //流正常结束之后的回调
                    System.out.println("流正常结束了");
                })
                .doOnCancel(() -> {
                    //流被取消后的回调
                    System.out.println("流被取消了");
                })
                .doOnTerminate(() -> {
                    //当流正常结束，或者报错时触发
                    System.out.println("流被终止运行了");
                })
                .doOnError(throwable -> {
                    //如果将 doOnError 放在可能会发生错误的操作前面，那么则不会进来该回调
                    //doOnError 的位置，要尽量放在最后，则它可以拿到（感知到）前面所有步骤的错误信息
                    //当流发生错误时的会回调
                    System.out.println("我是flux的报错了222" + throwable);
                });
        //订阅消费流
//        just.subscribe(System.out::println);
        just.subscribe(new MySubscribe());

    }

    /**
     * 订阅方法的测试
     */
    public static void subscribeTest() {
        Flux<Integer> range = Flux.range(1, 20);
        //只订阅，让流开始工作
        range.subscribe();
        //订阅，消费正常数据，并对流中的每个元素进行处理
        range.subscribe(v -> System.out.println("v=" + v));
        //订阅，处理元素，并处理 异常结束 的钩子
        range.subscribe(v -> System.out.println("v=" + v),
                throwable -> {
                    System.out.println("发生了异常" + throwable);
                });
        //订阅，处理每个元素，并处理 异常结束 和 正常完成 时触发的钩子
        range.subscribe(v -> System.out.println("v=" + v),
                throwable -> {
                    System.out.println("发生了异常" + throwable);
                },
                () -> {
                    System.out.println("触发onComplete，流正常完成时打印");
                });
        //订阅，并自定义每个钩子函数
        range.subscribe(new MySubscribe());
    }


    /**
     * 测试 背压(request) 和 请求重塑(buffer) 方法
     */
    public static void bufferTest(){
        Flux<List<Integer>> buffer = Flux.range(1, 10)
                //可以看到，加了 buffer 方法后，Flux 的泛型变成了 List<Integer>,而不是 Integer
                //目前 buffer 是2，则十个元素，一共只能被请求(被 request 方法调用) 10/2 = 5 次
                .buffer(2)//这句代码表示，缓冲区只能放两个元素，然后缓存区再满了之后，会将数据一次性发送给消费者,即消费者会收到一个数组
                ;

        //class java.util.ArrayList
        buffer.subscribe(v-> System.out.println(v.getClass()));
    }

    /**
     * 测试 limit 方法
     */
    public static void limitTest(){
        Flux<Integer> integerFlux = Flux.range(1, 300)
                //log 如果放在 limitRate 方法后面，那么看不出来该效果，是因为如果放在 limitRate 后面，则已经过了 limitRate 方法了，已经拿到所有元素了
                .log()
                //可以看到 log 打印了很多次  request(15)，第一次出现是在第15个元素之后，请求了一次 request(15)
                //为什么呢？
                //这是因为有一个 75% 的预取策略，即我的20个元素已经有 75% 被消费了，那么我会继续再获取 75% 个元素，其中 20*75% = 15
                .limitRate(20)//表示一次预取 20 个元素
                ;
        integerFlux.subscribe();
    }

    public static void main(String[] args) throws IOException {
//        flux();
//        mono();
//        subscribeTest();
//        methodTest();
//        bufferTest();
        limitTest();
        //卡住主线程别结束
        System.in.read();
    }


}
