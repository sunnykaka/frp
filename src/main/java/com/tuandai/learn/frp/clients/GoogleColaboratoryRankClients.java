package com.tuandai.learn.frp.clients;

import com.tuandai.learn.frp.domain.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@AllArgsConstructor
public class GoogleColaboratoryRankClients {

    @Getter
    private String zone;

    public IndexRankResult rankIndex(Set<ADIndex> indexs, QueryContext queryContext, UserBehavior userBehavior, UserExtend userExtend) {

        return new IndexRankResult();

    }

}
