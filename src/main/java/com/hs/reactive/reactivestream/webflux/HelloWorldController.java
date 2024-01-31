package com.hs.reactive.reactivestream.webflux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
public class HelloWorldController {

    @GetMapping("test")
    public Mono<String> hello(String test) {
        return Mono.just("xixi" + test);
    }

    //MediaType.TEXT_EVENT_STREAM_VALUE 标记为一个数据流，通知浏览器要一直接收
    @GetMapping(value = "event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Integer> eventTest() {
        return Flux.range(1, 20)
                .delayElements(Duration.ofSeconds(2));
    }

}
