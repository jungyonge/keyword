package com.keyword.util;


import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.data.relational.core.sql.In;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.Random;
import java.util.function.Consumer;

@Slf4j
public class Test {

    @FunctionalInterface
    public interface Math {
        public int Calc(int first, int second);
    }


    public static void main(String[] args) {

        Flux<Integer> seq = Flux.just(1, 2, 3); // Integer 값을 발생하는 Flux 생성



        seq.subscribe(v -> System.out.println("데이터 : " + v)); // 구독
    }
}
