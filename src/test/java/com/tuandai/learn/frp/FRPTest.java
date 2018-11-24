package com.tuandai.learn.frp;

import com.google.common.collect.Lists;
import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.QueryContext;
import com.tuandai.learn.frp.domain.UserQuery;
import com.tuandai.learn.frp.services.ADService;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;


public class FRPTest {

    private ADService adService = new ADService();

    private ADIndexClients adIndexClients = new ADIndexClients();

    private GoogleColaboratoryRankClients virginiaRankClients = new GoogleColaboratoryRankClients("Virginia");

    private GoogleColaboratoryRankClients californiaRankClients = new GoogleColaboratoryRankClients("California");

    private UserClients userClients = new UserClients();



    @Test
    public void testRxJava() {

        List<UserQuery> userQueryList = Lists.newArrayList(
                new UserQuery("RxJava", "1", new QueryContext()),
                new UserQuery("Spring", "2", new QueryContext())
        );

        ADQueryByRxJava2 adQuery = new ADQueryByRxJava2(adService, adIndexClients,
                virginiaRankClients, californiaRankClients, userClients);

        adQuery.process(userQueryList);


    }

}
