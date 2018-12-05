package com.tuandai.learn.frp.services;

import com.tuandai.learn.frp.domain.AD;
import com.tuandai.learn.frp.domain.ADIndex;
import com.tuandai.learn.frp.domain.IndexRankResult;

import java.util.HashMap;
import java.util.Map;

public class ADService {

    private final static Map<String, String> adContentMap = new HashMap<>();
    static {
        adContentMap.put("RxJava", "RxJava is a Java VM implementation of ReactiveX (Reactive Extensions): a library for composing asynchronous and event-based programs by using observable sequences.");
        adContentMap.put("Spring Framework", "The Spring Framework is an application framework and inversion of control container for the Java platform. ");
        adContentMap.put("Design Pattern", "In software engineering, a software design pattern is a general, reusable solution to a commonly occurring problem within a given context in software design");
        adContentMap.put("Lua", "Lua is a lightweight, multi-paradigm programming language designed primarily for embedded use in applications.");
        adContentMap.put("Java", "Java is a general-purpose computer-programming language that is concurrent, class-based, object-oriented,[15] and specifically designed to have as few implementation dependencies as possible.");
    }



    public AD findADbyRank(IndexRankResult indexRankResult) {

        ADIndex adIndex = indexRankResult.getAdIndices().get(0);

        return new AD(adIndex.getQuery(), adIndex.getIndexId(), adContentMap.get(adIndex.getQuery()));
    }



}
