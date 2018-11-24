package com.tuandai.learn.frp;

import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.*;
import com.tuandai.learn.frp.services.ADService;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Accessors(chain = true)
@RequiredArgsConstructor
public class ADQueryByRxJava2 {

    private final ADService adService;

    private final ADIndexClients adIndexClients;

    private final GoogleColaboratoryRankClients virginiaRankClients;

    private final GoogleColaboratoryRankClients californiaRankClients;

    private final UserClients userClients;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public List<AD> process(List<UserQuery> queryList) {

        Flowable<UserQuery> userQueries = Flowable.fromIterable(queryList);

        Flowable<Optional<UserExtend>> userExtends = userQueries.map(userQuery -> {
            return userClients.queryUserExtend(userQuery.getUserId());
        });

        Flowable<Optional<UserBehavior>> userBehaviors = userQueries.map(userQuery -> {
            return userClients.analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery());
        });

        Flowable<Set<ADIndex>> adIndicies = userQueries.map(userQuery ->
                adIndexClients.exactQuery(userQuery.getQuery())
        ).subscribeOn(Schedulers.newThread()).mergeWith(userQueries.map(userQuery ->
                adIndexClients.includeQuery(userQuery.getQuery())
        ).subscribeOn(Schedulers.newThread())).mergeWith(userQueries.map(userQuery ->
                adIndexClients.includeQuery(userQuery.getQuery())
        ).subscribeOn(Schedulers.newThread()));



        return null;

    }

}
