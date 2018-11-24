package com.tuandai.learn.frp.clients;

import com.tuandai.learn.frp.domain.*;

import java.util.Set;

public class GoogleColaboratoryRankClients {

    private String zone;

    public GoogleColaboratoryRankClients(String zone) {
        this.zone = zone;
    }

    public IndexRankResult rankIndex(Set<ADIndex> indexs, QueryContext queryContext, UserBehavior userBehavior, UserExtend userExtend) {

        return new IndexRankResult();

    }

    public String getZone() {
        return zone;
    }
}
