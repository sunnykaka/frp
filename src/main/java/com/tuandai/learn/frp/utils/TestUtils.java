package com.tuandai.learn.frp.utils;

import com.google.common.base.Stopwatch;

import java.util.concurrent.TimeUnit;

public interface TestUtils {

    static void wait(String className, String method) {

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            //模拟响应时间较慢
            Thread.sleep(3000L);
        } catch (InterruptedException ignore) {

        } finally {
            stopwatch.stop();
        }
        System.out.printf("%s方法%s.%s花费: %d ms \n", Thread.currentThread().getName(),
                className, method, stopwatch.elapsed(TimeUnit.MILLISECONDS));

    }



}
