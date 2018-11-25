package com.tuandai.learn.frp;

import com.google.common.collect.Lists;
import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.*;
import com.tuandai.learn.frp.services.ADService;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.functions.BiFunction;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

        Flowable<UeUbTuple> userInfoFlowable = Flowable.zip(
                userQueries.map(userQuery -> {
                    return userClients.queryUserExtend(userQuery.getUserId());
                }).subscribeOn(Schedulers.newThread()),
                userQueries.map(userQuery -> {
                    return userClients.analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery());
                }).subscribeOn(Schedulers.newThread()),
                (ue, ub) -> new UeUbTuple(ue.orElse(null), ub.orElse(null))
        );

//        Flowable<Optional<UserExtend>> userExtends = userQueries.map(userQuery -> {
//            return userClients.queryUserExtend(userQuery.getUserId());
//        }).subscribeOn(Schedulers.newThread()).zipWith(userQueries.map(userQuery -> {
//            return userClients.analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery());
//        }).subscribeOn(Schedulers.newThread()), (ue, ub) -> new UeUbTuple(ue.orElse(null), ub.orElse(null)));

        Flowable<Optional<UserBehavior>> userBehaviors = userQueries.map(userQuery -> {
            return userClients.analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery());
        });
        System.out.println("begin");

        Flowable<Set<ADIndex>> adIndices = userQueries.map(userQuery -> {
            Thread.sleep(3000L);
            System.out.println(Thread.currentThread());
            return adIndexClients.exactQuery(userQuery.getQuery());
        }).subscribeOn(Schedulers.newThread()).mergeWith(userQueries.map(userQuery -> {
            Thread.sleep(3000L);
            System.out.println(Thread.currentThread());
            return adIndexClients.includeQuery(userQuery.getQuery());
        }).subscribeOn(Schedulers.newThread())).mergeWith(userQueries.map(userQuery -> {
            Thread.sleep(3000L);
            System.out.println(Thread.currentThread());
            return adIndexClients.includeQuery(userQuery.getQuery());
        }).subscribeOn(Schedulers.newThread()));

//        Lists.newArrayList(adIndices.blockingIterable()
        Set<ADIndex> adIndexSet =
                StreamSupport.stream(adIndices.blockingIterable().spliterator(), false)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

//        Set<ADIndex> allSet = new HashSet<>();
//        adIndices.subscribe(s -> allSet.addAll(s));

//        try {
//            Thread.sleep(5000L);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        System.out.println(adIndexSet);
        System.out.println(adIndexSet.size());


        return null;

    }

    @AllArgsConstructor
    @Getter
    class UeUbTuple {

        UserExtend userExtend;

        UserBehavior userBehavior;

    }

}
