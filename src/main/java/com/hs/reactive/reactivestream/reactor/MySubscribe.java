package com.hs.reactive.reactivestream.reactor;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.SignalType;

public class MySubscribe extends BaseSubscriber<Integer> {
    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        System.out.println("消费订阅成功" + subscription);
        request(1);
    }

    @Override
    protected void hookOnNext(Integer value) {
        System.out.println("消费获取到消费的数据" + value);
//                if(value>8){
//                    //取消订阅流，触发 doOnCancel 方法
//                    cancel();
//                    //报个错，触发 hookOnError 方法
//                    int i = 10 / 0;
//                }
        request(1);
    }

    @Override
    protected void hookOnComplete() {
        System.out.println("消费正常完成");
        super.hookOnComplete();
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        System.out.println("消费错误完成" + throwable);
        super.hookOnError(throwable);
    }

    @Override
    protected void hookOnCancel() {
        System.out.println("消费订阅被取消");
        super.hookOnCancel();
    }

    @Override
    protected void hookFinally(SignalType type) {
        System.out.println("消费最终结果" + type);
        super.hookFinally(type);
    }
}
