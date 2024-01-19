package com.hs.reactive.reactivestream.flow;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * 底层思想使用了
 * 观察者模式（数据发生变化，通知所有的订阅者）+责任链模式
 * （将处理器1绑定到发布者中，再将处理器1绑定到处理器2中，再将订阅者绑定到处理器2中），则链条变为 发布者->处理器1->处理器2->订阅者
 * 观察者通知订阅者源码：
*                  BufferedSubscription<T> retries = null, rtail = null, next;
 *                 do {
 *                     next = b.next;
 *                     int stat = b.offer(item, unowned);
 *                     if (stat == 0) {              // saturated; add to retry list
 *                         b.nextRetry = null;       // avoid garbage on exceptions
 *                         if (rtail == null)
 *                             retries = b;
 *                         else
 *                             rtail.nextRetry = b;
 *                         rtail = b;
 *                     }
 *                     else if (stat < 0)            // closed
 *                         cleanMe = true;           // remove later
 *                     else if (stat > lag)
 *                         lag = stat;
 *                 } while ((b = next) != null);
 * 并增加了 异步线程处理，重试机制，异步锁竞争 的处理
 *
 * 更高级的总结：
 * 1、底层: 基于数据缓冲队列(发布者中的 buffer) + 消息驱动模型(观察者模式) + 异步回调机制（观察者模式,事件驱动）
 * 2、编码: 流式编程+链式调用+声明式API
 * 3、效果: 优雅全异步 + 消息实时处理 + 高吞吐量 + 占用少量资源
 */
public class FlowDemo {

    static class TestClass{
        String name;

        int age;
    }


    /**
     * 中间操作 处理器
     * 既是发布者，也是订阅者
     * 在这里，继承了发布者，所以只需要实现订阅者的接口
     */
    static class ProcessorTest extends SubmissionPublisher<String> implements Flow.Processor<String,String>{
        private Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            System.out.println("ProcessorTest拿到订阅关系"+subscription);
            this.subscription = subscription;
            //获取数据
            subscription.request(1);
        }

        @Override
        public void onNext(String item) {
            System.out.println("ProcessorTest 拿到数据"+item);
            //处理器对数据进行处理
            item = item+"这是 ProcessorTest 加上的内容：";
            //将加工处理后的数据，重新发出去
            submit(item);
            //继续获取数据
            subscription.request(1);
        }

        @Override
        public void onError(Throwable throwable) {
            System.out.println("ProcessorTest 报错了"+throwable);
        }

        @Override
        public void onComplete() {
            System.out.println("ProcessorTest onComplete");
        }
    }


    /**
     * Flow.Publisher 发布者
     * Flow.Subscriber 订阅者
     * Flow.Subscription 发布者和订阅者的绑定关系
     */
    public static void main(String[] args) {
        //1.定义发布者
//        Flow.Publisher<String> stringPublisher = new Flow.Publisher<>() {
//
//            private Flow.Subscriber<? super String> subscriber;
//
//            //订阅者会调用该方法来订阅发布者的消息
//            @Override
//            public void subscribe(Flow.Subscriber<? super String> subscriber) {
//                this.subscriber = subscriber;
//            }
//        };
        //jdk提供的简易发布者
        SubmissionPublisher<String> publisher=new SubmissionPublisher<>();

        //2.定义一个处理器
        Flow.Processor<String,String> processor = new ProcessorTest();

        //3.定义一个订阅者
        Flow.Subscriber<String> stringSubscriber = new Flow.Subscriber<>() {

            private Flow.Subscription subscription;

            /**
             * 发生绑定关系时触发
             * @param subscription a new subscription
             */
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                System.out.println("订阅绑定成功："+subscription);
                //从上游请求一个数据
                subscription.request(1);
                this.subscription = subscription;
            }

            /**
             * 收到下一个消息时触发
             * @param item the item
             */
            @Override
            public void onNext(String item) {
                System.out.println("接收到数据："+item);
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                if(!"3".equals(item)){
                    //拿到一个数据之后，继续请求下一个数据
                    subscription.request(1);
                }else{
                    //取消订阅
//                    subscription.cancel();
                }
            }

            /**
             * 订阅者接收到错误时，即发布者发生报错时
             * @param throwable the exception
             */
            @Override
            public void onError(Throwable throwable) {
                System.out.println("报错了："+throwable);
                subscription.request(1);
            }

            /**
             * 发布者 close 时触发
             */
            @Override
            public void onComplete() {
                System.out.println("完成！！");
            }
        };

        //4.绑定处理器，发布者和订阅者的关系
        //4.1 将处理器绑定到发布者中，再将订阅者绑定到处理器中
        //类似于责任链的绑定模式，因为处理器既是发布者，也是订阅者
        publisher.subscribe(processor);
        processor.subscribe(stringSubscriber);
        for (int i = 0; i < 5; i++) {
//            if(i==4){
//            //调用该方法，会中断
//                publisher.closeExceptionally(new RuntimeException("报错了，触发订阅者的OnError"));
//            }
            //所有数据都存在 SubmissionPublisher 的 buffer 缓冲区中
            //原理是使用 观察者 设计模式+线程池异步，去遍历所有订阅者来处理消息
            publisher.submit(String.valueOf(i));
        }
        //发布者关闭，会触发订阅者的 onComplete 方法
        publisher.close();
        try {
            Thread.sleep(2000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
