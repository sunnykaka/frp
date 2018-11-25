package com.tuandai.learn.frp.clients;

import com.google.common.collect.Sets;
import com.tuandai.learn.frp.domain.ADIndex;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashSet;
import java.util.Set;

public class ADIndexClients {

    public Set<ADIndex> exactQuery(String query) {
        return Sets.newHashSet(new ADIndex(query, "exact" + RandomStringUtils.randomAlphanumeric(8)));
    }


    public Set<ADIndex> includeQuery(String query) {
        return Sets.newHashSet(new ADIndex(query, "include" + RandomStringUtils.randomAlphanumeric(8)));
    }

    public Set<ADIndex> matchQuery(String query) {
        return Sets.newHashSet(new ADIndex(query, "match" + RandomStringUtils.randomAlphanumeric(8)));
    }

}
