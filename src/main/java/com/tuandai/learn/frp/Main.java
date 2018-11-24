package com.tuandai.learn.frp;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {

    private static BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>();


    public static void main(String[] args) throws InterruptedException {

        Flowable.create(e -> {
            for(int i = 0; i < 10000; i++) {
                e.onNext(i);
                System.out.println("here");
            }
        }, BackpressureStrategy.BUFFER)
                .subscribeOn(Schedulers.newThread())
                .observeOn(Schedulers.from(new Runner()))
                .subscribe(System.out::println);

        System.out.println("take");
        while(!Thread.interrupted()){
            System.out.println("in for");
            Runnable task = tasks.take();
            task.run();
        }

        System.out.println("finish");

    }

    static class Runner implements Executor {



        @Override
        public void execute(Runnable command) {
            System.out.println(tasks.offer(command));
        }
    }


}

