package com.tuandai.learn.frp;

import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.*;
import com.tuandai.learn.frp.services.ADService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class RequestProcessor {

    private ADService adService;

    private ADIndexClients adIndexClients;

    private GoogleColaboratoryRankClients virginiaRankClients;

    private GoogleColaboratoryRankClients californiaRankClients;

    private UserClients userClients;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public RequestProcessor(ADService adService, ADIndexClients adIndexClients, GoogleColaboratoryRankClients virginiaRankClients,
                            GoogleColaboratoryRankClients californiaRankClients, UserClients userClients) {
        this.adService = adService;
        this.adIndexClients = adIndexClients;
        this.virginiaRankClients = virginiaRankClients;
        this.californiaRankClients = californiaRankClients;
        this.userClients = userClients;
    }

    public AD process(String query, String userId, QueryContext queryContext) {

        //
        Optional<UserExtend> userExtend = userClients.queryUserExtend(userId);
        Optional<UserBehavior> userBehavior = userClients.analyzeUserBehaviorByQuery(userId, query);

        Set<ADIndex> exactIndices = adIndexClients.exactQuery(query);
        Set<ADIndex> includeIndices = adIndexClients.includeQuery(query);
        Set<ADIndex> matchIndices = adIndexClients.matchQuery(query);

        Set<ADIndex> allIndices = new HashSet<>();
        allIndices.addAll(exactIndices);
        allIndices.addAll(includeIndices);
        allIndices.addAll(matchIndices);


        BlockingQueue<IndexRankResult> rankResultQueue = new LinkedBlockingQueue<>();

        executorService.submit(() -> {
            rankResultQueue.add(virginiaRankClients.rankIndex(allIndices, queryContext, userBehavior.orElse(null), userExtend.orElse(null)));
        });
        executorService.submit(() -> {
            rankResultQueue.add(californiaRankClients.rankIndex(allIndices, queryContext, userBehavior.orElse(null), userExtend.orElse(null)));
        });

        IndexRankResult rankResult = null;
        while(rankResult == null) {
            try {
                rankResult = rankResultQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return adService.findADbyRank(rankResult);

    }

}
