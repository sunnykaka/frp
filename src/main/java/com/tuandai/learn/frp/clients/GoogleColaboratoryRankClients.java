package com.tuandai.learn.frp.clients;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.tuandai.learn.frp.domain.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@AllArgsConstructor
public class GoogleColaboratoryRankClients {

    @Getter
    private String zone;

    public IndexRankResult rankIndex(Set<ADIndex> indexs, QueryContext queryContext, UserBehavior userBehavior, UserExtend userExtend) {

        //模拟响应慢
        wait("rankIndex");

        List<ADIndex> results = new ArrayList<>(indexs);
        results.sort(Comparator.comparing(ADIndex::getIndexId));

        return new IndexRankResult(results);

    }

    private void wait(String method) {

        int sleepSeconds = 10;
        if(zone.equalsIgnoreCase("Virginia")) {
            //假设与virginia区域的服务通信非常慢
            sleepSeconds = 120;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            System.out.printf("zone: %s, 方法GoogleColaboratoryRankClients.%s start waiting, %s \n", zone, method, LocalDateTime.now());
            //模拟响应时间较慢
            Thread.sleep(sleepSeconds * 1000);
        } catch (InterruptedException ignore) {
            ignore.printStackTrace();

        } finally {
            stopwatch.stop();
        }
        System.out.printf("zone: %s, 方法GoogleColaboratoryRankClients.%s花费: %d ms \n", zone, method, stopwatch.elapsed(TimeUnit.MILLISECONDS));

    }


}
