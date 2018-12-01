package com.tuandai.learn.frp.clients;

import com.tuandai.learn.frp.domain.UserBehavior;
import com.tuandai.learn.frp.domain.UserExtend;
import com.tuandai.learn.frp.utils.TestUtils;

import java.util.Optional;

public class UserClients {

    public Optional<UserBehavior> analyzeUserBehaviorByQuery(String userId, String query) {
        TestUtils.wait("UserClients", "analyzeUserBehaviorByQuery");
        return Optional.of(new UserBehavior(userId, "userBehavior"));
    }

    public Optional<UserExtend> queryUserExtend(String userId) {
        TestUtils.wait("UserClients", "queryUserExtend");
        return Optional.of(new UserExtend(userId, "otherInfo"));
    }



}
