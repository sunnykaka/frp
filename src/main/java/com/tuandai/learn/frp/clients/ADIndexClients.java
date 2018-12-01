package com.tuandai.learn.frp.clients;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.tuandai.learn.frp.domain.ADIndex;
import com.tuandai.learn.frp.utils.TestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ADIndexClients {

    public Set<ADIndex> exactQuery(String query) {
        TestUtils.wait("ADIndexClients", "exactQuery");
        return Sets.newHashSet(new ADIndex(query, "exact-" + RandomStringUtils.randomAlphanumeric(8)));
    }


    public Set<ADIndex> includeQuery(String query) {
        TestUtils.wait("ADIndexClients", "includeQuery");
        return Sets.newHashSet(new ADIndex(query, "include-" + RandomStringUtils.randomAlphanumeric(8)));
    }

    public Set<ADIndex> matchQuery(String query) {
        TestUtils.wait("ADIndexClients", "matchQuery");
        return Sets.newHashSet(new ADIndex(query, "match1-" + RandomStringUtils.randomAlphanumeric(8)),
                new ADIndex(query, "match2-" + RandomStringUtils.randomAlphanumeric(8)));
    }

}
