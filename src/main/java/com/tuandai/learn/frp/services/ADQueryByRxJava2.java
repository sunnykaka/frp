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

        //获取用户基础信息和行为信息
        Flowable<UserInfoResults> userInfoFlowable = Flowable.zip(
                userQueries.map(userQuery -> userClients.queryUserExtend(userQuery.getUserId())).sequential(),
                userQueries.map(userQuery -> userClients.
                        analyzeUserBehaviorByQuery(userQuery.getUserId(), userQuery.getQuery())).sequential(),
                (ue, ub) -> new UserInfoResults(ue.orElse(null), ub.orElse(null))
        );

        //查询语句匹配
        Flowable<Set<ADIndex>> queryMatchResults = Flowable.zip(
                userQueries.map(userQuery -> adIndexClients.exactQuery(userQuery.getQuery())).sequential(),
                userQueries.map(userQuery -> adIndexClients.includeQuery(userQuery.getQuery())).sequential(),
                userQueries.map(userQuery -> adIndexClients.matchQuery(userQuery.getQuery())).sequential(),
                (x, y, z) -> Stream.of(x, y, z).flatMap(Set::stream).collect(Collectors.toSet())
        );

        //创建的rankQuery数量
        final AtomicInteger rankQueryProduceCount = new AtomicInteger(0);
        //已处理的rankQuery数量
        final AtomicInteger rankQueryConsumeCount = new AtomicInteger(0);
        //rankQuery是否已经创建完成
        final AtomicBoolean produceFinished  = new AtomicBoolean(false);
        BlockingQueue<RankQuery> rankQueryQueue = new LinkedBlockingQueue<>();
        Map<String, IndexRankResult> indexRankResultMap = new ConcurrentHashMap<>();

        //合并queryList, userInfoFlowable, queryMatchResults三个流的数据进行处理
        Flowable.zip(Flowable.fromIterable(queryList), userInfoFlowable, queryMatchResults,
                (userQuery, userInfo, adIndices) -> new RankQuery(userQuery, userInfo.userExtend, userInfo.userBehavior, adIndices)
        ).doOnComplete(() -> produceFinished.set(true)).subscribe(rankQuery -> {
            rankQueryProduceCount.getAndIncrement();
            rankQueryQueue.add(rankQuery);
        });

        //只要rankQuery还没有全部创建完成, 或者已处理的数量不等于已创建的数量, 继续循环
        while(!produceFinished.get() || rankQueryConsumeCount.get() != rankQueryProduceCount.get()) {
            //尝试从队列中获取数据, 如果没有数据等待100ms后返回null
            RankQuery rankQuery = rankQueryQueue.poll(100L, TimeUnit.MILLISECONDS);
            if(rankQuery != null) {
                //请求google colab服务进行rank, 因为地域较远并且请求耗时不确定, 我们请求了两个位于不同地理位置的google colab服务.
                //只要有一个请求返回结果, 我们就继续往下执行.
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
