package com.tuandai.learn.frp.clients;

import com.tuandai.learn.frp.domain.UserBehavior;
import com.tuandai.learn.frp.domain.UserExtend;

import java.util.Optional;

public class UserClients {

    public Optional<UserBehavior> analyzeUserBehaviorByQuery(String userId, String query) {
        return Optional.ofNullable(new UserBehavior());
    }

    public Optional<UserExtend> queryUserExtend(String userId) {
        return Optional.ofNullable(new UserExtend());
    }


}
