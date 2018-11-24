package com.tuandai.learn.frp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserQuery {

    private String query;

    private String userId;

    private QueryContext queryContext;

}
