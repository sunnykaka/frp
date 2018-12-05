package com.tuandai.learn.frp.services;

import com.tuandai.learn.frp.domain.AD;
import com.tuandai.learn.frp.domain.UserQuery;

import java.util.List;

public interface ADQueryIF {

    /**
     /**
     * 1. 用户输入搜索条件 -> 2. 从用户行为分析服务获取用户基础信息和用户行为信息 ->
     * 3. 从广告索引服务获取搜索语句的索引精确匹配, 短语匹配, 广泛匹配结果 -> 4. 请求google colaboratory 进行index rank
     * 5. 根据rank结果, 从广告数据库服务查询广告并返回
     * @param queryList
     * @return
     * @throws Exception
     */
    List<AD> process(List<UserQuery> queryList) throws Exception;

}
