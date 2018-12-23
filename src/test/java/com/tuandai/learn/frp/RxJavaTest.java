package com.tuandai.learn.frp;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.AD;
import com.tuandai.learn.frp.domain.QueryContext;
import com.tuandai.learn.frp.domain.UserQuery;
import com.tuandai.learn.frp.services.ADQueryByPlain;
import com.tuandai.learn.frp.services.ADQueryByRxJava2;
import com.tuandai.learn.frp.services.ADQueryIF;
import com.tuandai.learn.frp.services.ADService;
import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class RxJavaTest {

    @Test
    public void observableDemo() throws Exception {

        Observable.create((ObservableOnSubscribe<String>) source -> {
            source.onNext("data 1");
            source.onNext("data 2");
            source.onNext("data 3");
        }).map(String::toUpperCase)
                .filter(data -> !data.contains("3"))
                .doOnComplete(() -> System.out.println("complete!"))
                .subscribe(System.out::println);


    }

    @Test
    public void backPressureDemo() throws Exception {

        Flowable.create(source -> {
            for(int i = 1; i <= 129; i++) {
                source.onNext(i);
            }
        }, BackpressureStrategy.ERROR)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.newThread())
                .subscribe(System.out::println);

        Thread.sleep(1000L);

    }



}
