package com.hs.reactive.reactivestream.reactor;

import reactor.core.publisher.Flux;

/**
 * 错误处理 <br/>
 * 1. onErrorReturn     【流会正常完成，消费者不会收到异常】 <br/>
 * 2. onErrorResume     【流会正常完成，消费者不会收到异常】<br/>
 * 3. onErrorMap        【流会异常完成，消费者会收到异常】 <br/>
 * 4. doOnError         【流会异常完成，消费者会收到异常】 <br/>
 * 5. onErrorContinue   【流会正常完成，消费者不会收到异常】 <br/>
 * 6. onErrorComplete   【流会正常完成，消费者不会收到异常】 <br/>
 * 7. onErrorStop       【流会异常完成，但消费者不会收到异常】
 */
public class FluxErrorHandle {

    static void errorTest() {
        Flux<Integer> integerFlux = Flux.range(1, 10)
                .map(s -> {
                    if (s == 3) {
                        s = s / 0;
                    }
                    return s;
                })
                // 1. onErrorReturn 当发生错误时，返回一个默认值 -1,类似于 try catch中，catch到了错误，【流会正常完成，消费者不会收到异常】
                // catch(Exception e){
                //  return -1;
                // }
//                .onErrorReturn(-1)
                // 2. onErrorResume 2.1 当发生错误时，返回一个新的流继续操作，【流会正常完成，消费者不会收到异常】
//                .onErrorResume(e->{
//                    if(e instanceof Exception){
//                        return Flux.range(10,11);
//                    }else{
//                        return Flux.range(0,0);
//                    }
//                })
                //2. onErrorResume 2.2 当发生错误时，将异常包装为另外一个异常，【流会异常完成,消费者会收到异常】
//                .onErrorResume(e ->
//                        Flux.error(new NullPointerException(e.getMessage()))
//                )
                //3. onErrorMap 当发生错误时，将异常包装为另外一个异常，【流会异常完成，消费者会收到异常】
//                .onErrorMap(throwable -> new MyException(throwable.getMessage()+"我的异常信息"))
                //4. doOnError 当发生异常时，进入我的回调函数，不干预 流的结果，会异常完成【流会异常完成，消费者会收到异常】
                .doOnError(throwable -> System.out.println(throwable.getMessage()+"报错咯"))
                //5. 不管上面的流怎么结束，都会触发，并返回结束的类型，如果下面的流 使用了 onErrorComplete ，那么这里也会收到 onError 的结果，因为它只关心上面的流
                .doFinally(signalType -> {
                    System.out.println("执行结果"+signalType);
                })
                //6. onErrorContinue 忽略当前异常，不打断流，只进入该回调，然后继续跑下面的流元素【流会正常完成，消费者不会收到异常】
//                .onErrorContinue((err,value)->{
//                    System.out.println("元素"+value+"出现了问题"+err+"，我记录一个日志，或者发送一个通知，但不会影响我继续执行");
//                })
                //7. onErrorComplete，发生异常时，算流正常结束【流会正常完成，消费者不会收到异常】
//                .onErrorComplete()
                //8. 当发生错误时，直接停止掉流，所有的监听者全部结束掉【流会异常完成，但消费者不会收到异常】
                .onErrorStop()
                ;
        integerFlux.subscribe(System.out::println,err-> System.out.println("消费者收到异常："+err.getMessage()));
    }


    public static void main(String[] args) {
        errorTest();
    }
}
