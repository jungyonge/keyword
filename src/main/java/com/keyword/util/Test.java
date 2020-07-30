package com.keyword.util;


import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class Test {

    public static void main(String[] args) {
        Flux<Integer> seq = Flux.just(1, 2, 3);
        seq.subscribe(value -> log.info("데이터 : " + value));

    }

}
