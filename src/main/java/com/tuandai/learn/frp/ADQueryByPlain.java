package com.tuandai.learn.frp;

import com.tuandai.learn.frp.clients.ADIndexClients;
import com.tuandai.learn.frp.clients.GoogleColaboratoryRankClients;
import com.tuandai.learn.frp.clients.UserClients;
import com.tuandai.learn.frp.domain.*;
import com.tuandai.learn.frp.services.ADService;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Accessors(chain = true)
@RequiredArgsConstructor
public class ADQueryByPlain {

    private final ADService adService;

    private final ADIndexClients adIndexClients;

    private final GoogleColaboratoryRankClients virginiaRankClients;

    private final GoogleColaboratoryRankClients californiaRankClients;

    private final UserClients userClients;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public List<AD> process(List<UserQuery> queryList) {

        List<AD> adList = new ArrayList<>(queryList.size());

        for(UserQuery userQuery : queryList) {

            String userId = userQuery.getUserId();
            String query = userQuery.getQuery();
            QueryContext queryContext = userQuery.getQueryContext();

            //获取用户基础信息扩展
            Optional<UserExtend> userExtend = userClients.queryUserExtend(userId);
            //获取用户行为信息扩展
            Optional<UserBehavior> userBehavior = userClients.analyzeUserBehaviorByQuery(userId, query);

            //索引精确匹配
            Set<ADIndex> exactIndices = adIndexClients.exactQuery(query);
            //短语匹配
            Set<ADIndex> includeIndices = adIndexClients.includeQuery(query);
            //广泛匹配
            Set<ADIndex> matchIndices = adIndexClients.matchQuery(query);

            Set<ADIndex> allIndices = new HashSet<>();
            allIndices.addAll(exactIndices);
            allIndices.addAll(includeIndices);
            allIndices.addAll(matchIndices);


            BlockingQueue<IndexRankResult> rankResultQueue = new LinkedBlockingQueue<>();

            //请求google colab服务进行rank, 因为地域较远并且请求耗时不确定, 我们请求了两个位于不同地理位置的google colab服务.
            //只要有一个请求返回结果, 我们就继续往下执行.
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

            //根据rank结果查询数据库, 返回广告结果.
            AD ad = adService.findADbyRank(rankResult);

            adList.add(ad);
        }

        return adList;

    }

}
