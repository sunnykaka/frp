package com.tuandai.learn.frp;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.IndexRankResult;
import com.tuandai.learn.frp.domain.QueryContext;
import com.tuandai.learn.frp.domain.UserQuery;
import com.tuandai.learn.frp.services.ADService;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class FRPTest {

    private ADService adService = new ADService();

    private ADIndexClients adIndexClients = new ADIndexClients();

    private GoogleColaboratoryRankClients virginiaRankClients = new GoogleColaboratoryRankClients("Virginia");

    private GoogleColaboratoryRankClients californiaRankClients = new GoogleColaboratoryRankClients("California");

    private UserClients userClients = new UserClients();

    @Test
    public void testPlainJava() throws InterruptedException, TimeoutException, ExecutionException {

        Stopwatch stopwatch = Stopwatch.createStarted();

        IndexRankResult indexRankResult = (IndexRankResult) CompletableFuture.anyOf(
                CompletableFuture.supplyAsync(() -> virginiaRankClients.rankIndex(new HashSet<>(),
                        null, null, null)),
                CompletableFuture.supplyAsync(() -> californiaRankClients.rankIndex(new HashSet<>(),
                        null, null, null))
        ).get(60, TimeUnit.SECONDS);

        stopwatch.stop();

        System.out.println("耗时: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        System.out.println(indexRankResult);

    }



    @Test
    public void testRxJava() throws InterruptedException {

        List<UserQuery> userQueryList = Lists.newArrayList(
                new UserQuery("RxJava", "1", new QueryContext()),
                new UserQuery("Spring", "2", new QueryContext()),
                new UserQuery("Design Pattern", "3", new QueryContext())
        );

        ADQueryByRxJava2 adQuery = new ADQueryByRxJava2(adService, adIndexClients,
                virginiaRankClients, californiaRankClients, userClients);

        adQuery.process(userQueryList);


    }



}
