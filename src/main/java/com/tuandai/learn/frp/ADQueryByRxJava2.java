package com.tuandai.learn.frp;

import com.google.common.base.Stopwatch;
import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.*;
import com.tuandai.learn.frp.services.ADService;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.parallel.ParallelFlowable;
import io.reactivex.schedulers.Schedulers;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    /**
     * 1. 用户输入搜索条件 -> 2. 从用户行为分析服务获取用户基础信息和用户行为信息 ->
     * 3. 从广告索引服务获取搜索语句的索引精确匹配, 短语匹配, 广泛匹配结果 -> 4. 请求google colaboratory 进行index rank
     * 5. 根据rank结果, 从广告数据库服务查询广告并返回
     *
     * @param queryList
     * @return
     */
    public List<AD> process(List<UserQuery> queryList) throws InterruptedException {

        ParallelFlowable<UserQuery> userQueries = Flowable.fromIterable(queryList).parallel().runOn(Schedulers.newThread());

        Flowable<UserInfoResults> userInfoFlowable = Flowable.zip(
                userQueries.map(userQuery -> userClients.queryUserExtend(userQuery.getUserId()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                userQueries.map(userQuery -> userClients.analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                (ue, ub) -> new UserInfoResults(ue.orElse(null), ub.orElse(null))
        );

//        BlockingQueue<IndexRankResult> rankResultQueue = new LinkedBlockingQueue<>();
//
//        Stopwatch stopwatch = Stopwatch.createStarted();
//
//        Flowable<Set<ADIndex>> queryMatchResults = Flowable.zip(
//                userQueries.map(userQuery -> {
//                    System.out.println(Thread.currentThread());
//                    return adIndexClients.exactQuery(userQuery.getQuery());
//                }).subscribeOn(Schedulers.newThread()),
//                userQueries.map(userQuery -> {
//                    System.out.println(Thread.currentThread());
//                    return adIndexClients.includeQuery(userQuery.getQuery());
//                }).subscribeOn(Schedulers.newThread()),
//                userQueries.map(userQuery -> {
//                    System.out.println(Thread.currentThread());
//                    return adIndexClients.matchQuery(userQuery.getQuery());
//                }).subscribeOn(Schedulers.newThread()),
//                (x, y, z) -> Stream.of(x, y, z).flatMap(Set::stream).collect(Collectors.toSet())
//        );
//
//        Flowable.zip(userQueries, userInfoFlowable, queryMatchResults,
//                (userQuery, userInfo, adIndices) -> new RankQuery(userQuery, userInfo.userExtend, userInfo.userBehavior, adIndices)
//        ).map(rankQuery ->
//                (IndexRankResult) CompletableFuture.anyOf(
//                        CompletableFuture.supplyAsync(() -> virginiaRankClients.rankIndex(rankQuery.adIndices, rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend)),
//                        CompletableFuture.supplyAsync(() -> californiaRankClients.rankIndex(rankQuery.adIndices, rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend))
//                ).get(60, TimeUnit.SECONDS)
//        ).subscribeOn(Schedulers.newThread()).subscribe(x -> rankResultQueue.add(x));
//
//        int count = 0;
//        while(true) {
//            IndexRankResult indexRankResult = rankResultQueue.take();
//            System.out.printf("第%d个结果, 当前耗时%d ms\n", ++count, stopwatch.elapsed(TimeUnit.MILLISECONDS));
//            System.out.println("indexRankResult: " + indexRankResult);
//        }


//        Set<ADIndex> adIndexSet =
//                StreamSupport.stream(userInfoFlowable.blockingIterable().spliterator(), false)
//                        .flatMap(Set::stream)
//                        .collect(Collectors.toSet());


//        Flowable<Optional<UserExtend>> userExtends = userQueries.map(userQuery -> {
//            return userClients.queryUserExtend(userQuery.getUserId());
//        }).subscribeOn(Schedulers.newThread()).zipWith(userQueries.map(userQuery -> {
//            return userClients.analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery());
//        }).subscribeOn(Schedulers.newThread()), (ue, ub) -> new UserInfoResults(ue.orElse(null), ub.orElse(null)));

//        Flowable<Optional<UserBehavior>> userBehaviors = userQueries.map(userQuery -> {
//            return userClients.analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery());
//        });

        BlockingQueue<RankQuery> rankQueryQueue = new LinkedBlockingQueue<>();

        Stopwatch stopwatch = Stopwatch.createStarted();

        Flowable<Set<ADIndex>> queryMatchResults = Flowable.zip(
                userQueries.map(userQuery -> adIndexClients.exactQuery(userQuery.getQuery()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                userQueries.map(userQuery -> adIndexClients.includeQuery(userQuery.getQuery()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                userQueries.map(userQuery -> adIndexClients.matchQuery(userQuery.getQuery()))
                        .sequential().subscribeOn(Schedulers.newThread()),
                (x, y, z) -> Stream.of(x, y, z).flatMap(Set::stream).collect(Collectors.toSet())
        );

        final AtomicInteger count = new AtomicInteger(0);

        Flowable.zip(Flowable.fromIterable(queryList).subscribeOn(Schedulers.newThread()), userInfoFlowable.subscribeOn(Schedulers.newThread()), queryMatchResults.subscribeOn(Schedulers.newThread()),
                (userQuery, userInfo, adIndices) -> {
                    System.out.println("start zip: " + LocalDateTime.now());
                    Thread.sleep(3000L);
                    return new RankQuery(userQuery, userInfo.userExtend, userInfo.userBehavior, adIndices);
                }
        ).parallel().runOn(Schedulers.newThread()).sequential().subscribe(rankQuery -> {
            System.out.println("start subscribe: " + LocalDateTime.now());
            Thread.sleep(3000L);

//            IndexRankResult x = (IndexRankResult) CompletableFuture.anyOf(
//                    CompletableFuture.supplyAsync(() -> virginiaRankClients.rankIndex(rankQuery.adIndices,
//                            rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend)),
//                    CompletableFuture.supplyAsync(() -> californiaRankClients.rankIndex(rankQuery.adIndices,
//                            rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend))
//            ).get(60, TimeUnit.SECONDS);
//            System.out.printf("第%d个结果, 当前耗时%d ms\n", count.addAndGet(1), stopwatch.elapsed(TimeUnit.MILLISECONDS));
//            System.out.println("indexRankResult: " + x);
        });

        System.out.printf("第%d个结果, 当前耗时%d ms\n", count.addAndGet(1), stopwatch.elapsed(TimeUnit.MILLISECONDS));

        Thread.sleep(30000L);
//        ).parallel().runOn(Schedulers.newThread()
////                californiaRankClients.rankIndex(rankQuery.adIndices, rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend)
//        ).sequential().subscribe(rankQuery -> {
//            IndexRankResult x = (IndexRankResult) CompletableFuture.anyOf(
//                    CompletableFuture.supplyAsync(() -> virginiaRankClients.rankIndex(rankQuery.adIndices,
//                            rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend)),
//                    CompletableFuture.supplyAsync(() -> californiaRankClients.rankIndex(rankQuery.adIndices,
//                            rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend))
//            ).get(60, TimeUnit.SECONDS);
//            System.out.printf("第%d个结果, 当前耗时%d ms\n", count.addAndGet(1), stopwatch.elapsed(TimeUnit.MILLISECONDS));
//            System.out.println("indexRankResult: " + x);
//        });


//        Flowable.zip(Flowable.fromIterable(queryList), userInfoFlowable, queryMatchResults,
//                (userQuery, userInfo, adIndices) -> new RankQuery(userQuery, userInfo.userExtend, userInfo.userBehavior, adIndices)
//        ).parallel().runOn(Schedulers.newThread()).map(rankQuery ->
////                californiaRankClients.rankIndex(rankQuery.adIndices, rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend)
//                (IndexRankResult) CompletableFuture.anyOf(
//                        CompletableFuture.supplyAsync(() -> virginiaRankClients.rankIndex(rankQuery.adIndices,
//                                rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend)),
//                        CompletableFuture.supplyAsync(() -> californiaRankClients.rankIndex(rankQuery.adIndices,
//                                rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend))
//                ).get(60, TimeUnit.SECONDS)
//        ).sequential().blockingIterable().forEach(x -> {
//            System.out.printf("第%d个结果, 当前耗时%d ms\n", count.addAndGet(1), stopwatch.elapsed(TimeUnit.MILLISECONDS));
//            System.out.println("indexRankResult: " + x);
//        });


//        StreamSupport.stream(rankQueries.spliterator(), false).map(rankQuery -> {
//
//            CompletableFuture<Object> future = CompletableFuture.anyOf(
//                    CompletableFuture.supplyAsync(() -> virginiaRankClients.rankIndex(rankQuery.adIndices, rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend)),
//                    CompletableFuture.supplyAsync(() -> californiaRankClients.rankIndex(rankQuery.adIndices, rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend))
//            );
//
//        })




//        Flowable.zip(userQueries, userInfoFlowable, queryMatchResults,
//                (userQuery, userInfo, adIndices) -> new RankQuery(userQuery, userInfo.userExtend, userInfo.userBehavior, adIndices)
//        ).map(rankQuery ->
//                (IndexRankResult) CompletableFuture.anyOf(
//                        CompletableFuture.supplyAsync(() -> virginiaRankClients.rankIndex(rankQuery.adIndices, rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend)),
//                        CompletableFuture.supplyAsync(() -> californiaRankClients.rankIndex(rankQuery.adIndices, rankQuery.userQuery.getQueryContext(), rankQuery.userBehavior, rankQuery.userExtend))
//                ).get(60, TimeUnit.SECONDS)
//        ).subscribeOn(Schedulers.newThread())
//        .subscribe(x -> System.out.println(x));


//        Thread.sleep(20000L);
//
//        System.out.println("indexRankResult: " + indexRankResult);



//        List<Set<ADIndex>> allAdIndices =
//                StreamSupport.stream(adIndices.blockingIterable().spliterator(), false)
////                .flatMap(Set::stream)
//                .collect(Collectors.toList());


//        System.out.println(allAdIndices);
//        System.out.println(allAdIndices.size());
//        System.out.println("耗时: " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");


        return null;

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
    class QueryMatchResults {

        Set<ADIndex> exactResults;

        Set<ADIndex> includeResults;

        Set<ADIndex> matchResults;

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
