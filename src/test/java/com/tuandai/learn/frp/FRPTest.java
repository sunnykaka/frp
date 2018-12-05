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
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;


public class FRPTest {

    private ADService adService = new ADService();

    private ADIndexClients adIndexClients = new ADIndexClients();

    private GoogleColaboratoryRankClients virginiaRankClients = new GoogleColaboratoryRankClients("Virginia");

    private GoogleColaboratoryRankClients californiaRankClients = new GoogleColaboratoryRankClients("California");

    private UserClients userClients = new UserClients();

    @Test
    public void testPlainJava() throws Exception  {

        runTest(new ADQueryByPlain(adService, adIndexClients,
                virginiaRankClients, californiaRankClients, userClients));

    }

    @Test
    public void testRxJava() throws Exception {

        runTest(new ADQueryByRxJava2(adService, adIndexClients,
                virginiaRankClients, californiaRankClients, userClients));
    }

    private void runTest(ADQueryIF adQuery) throws Exception {

        List<UserQuery> userQueryList = Lists.newArrayList(
                new UserQuery("RxJava", "1", new QueryContext()),
                new UserQuery("Spring Framework", "2", new QueryContext()),
                new UserQuery("Design Pattern", "3", new QueryContext())
        );

        Stopwatch stopwatch = Stopwatch.createStarted();
        List<AD> adList = null;

        try {
            adList = adQuery.process(userQueryList);

        } finally {
            stopwatch.stop();
            System.out.println("耗时: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
            System.out.println(adList);

        }
    }



}
