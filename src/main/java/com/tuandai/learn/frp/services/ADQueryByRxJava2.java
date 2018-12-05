package com.tuandai.learn.frp.services;

import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.*;
import io.reactivex.Flowable;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Accessors(chain = true)
@RequiredArgsConstructor
public class ADQueryByRxJava2 implements ADQueryIF  {

    private final ADService adService;

    private final ADIndexClients adIndexClients;

    private final GoogleColaboratoryRankClients virginiaRankClients;

    private final GoogleColaboratoryRankClients californiaRankClients;

    private final UserClients userClients;

    private ExecutorService es = Executors.newCachedThreadPool();

    @Override
    public List<AD> process(List<UserQuery> queryList) throws InterruptedException {

        ParallelFlowable<UserQuery> userQueries = Flowable.fromIterable(queryList).parallel().runOn(Schedulers.newThread());

        Flowable<UserInfoResults> userInfoFlowable = Flowable.zip(
                userQueries.map(userQuery -> userClients.queryUserExtend(userQuery.getUserId()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                userQueries.map(userQuery -> userClients.analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                (ue, ub) -> new UserInfoResults(ue.orElse(null), ub.orElse(null))
        );

        Flowable<Set<ADIndex>> queryMatchResults = Flowable.zip(
                userQueries.map(userQuery -> adIndexClients.exactQuery(userQuery.getQuery()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                userQueries.map(userQuery -> adIndexClients.includeQuery(userQuery.getQuery()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                userQueries.map(userQuery -> adIndexClients.matchQuery(userQuery.getQuery()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                (x, y, z) -> Stream.of(x, y, z).flatMap(Set::stream).collect(Collectors.toSet())
        );

        final AtomicInteger rankQueryProduceCount = new AtomicInteger(0);
        final AtomicInteger rankQueryConsumeCount = new AtomicInteger(0);
        final AtomicBoolean produceFinished  = new AtomicBoolean(false);
        BlockingQueue<RankQuery> rankQueryQueue = new LinkedBlockingQueue<>();
        Map<String, IndexRankResult> indexRankResultMap = new ConcurrentHashMap<>();

        Flowable.zip(Flowable.fromIterable(queryList), userInfoFlowable, queryMatchResults,
                (userQuery, userInfo, adIndices) -> new RankQuery(userQuery, userInfo.userExtend, userInfo.userBehavior, adIndices)
        ).doOnComplete(() -> produceFinished.set(true)).subscribe(rankQuery -> {
            rankQueryProduceCount.getAndIncrement();
            rankQueryQueue.add(rankQuery);
        });

        while(!produceFinished.get() || rankQueryConsumeCount.get() != rankQueryProduceCount.get()) {
            RankQuery rankQuery = rankQueryQueue.poll(1000L, TimeUnit.MILLISECONDS);
            if(rankQuery != null) {
                CompletableFuture.anyOf(
                        CompletableFuture.supplyAsync(() -> virginiaRankClients.rankIndex(rankQuery.adIndices,
                                rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend), es),
                        CompletableFuture.supplyAsync(() -> californiaRankClients.rankIndex(rankQuery.adIndices,
                                rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend), es)
                ).whenCompleteAsync((result, ignoreEx) -> {
                    indexRankResultMap.put(rankQuery.getUserQuery().getQuery(), (IndexRankResult) result);
                    rankQueryConsumeCount.getAndIncrement();
                }, es);
            }
        }

        return queryList.stream().map(userQuery -> indexRankResultMap.get(userQuery.getQuery()))
                .map(adService::findADbyRank).collect(Collectors.toList());

    }

    @AllArgsConstructor
    @Getter
    @ToString
    class UserInfoResults {

        UserExtend userExtend;

        UserBehavior userBehavior;

    }

    @AllArgsConstructor
    @Getter
    @ToString
    class RankQuery {

        UserQuery userQuery;

        UserExtend userExtend;

        UserBehavior userBehavior;

        Set<ADIndex> adIndices;

    }



}
