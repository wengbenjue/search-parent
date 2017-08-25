package com.aug3.storage.mongoclient.aggregate.builder.impl;

import com.aug3.storage.mongoclient.aggregate.AggregationPipline;
import com.aug3.storage.mongoclient.aggregate.builder.MatchBuilder;
import com.mongodb.BasicDBObject;

import java.util.Map;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public class BasicMatchBuilder implements MatchBuilder {

    private final AggregationPipline pipline;
    private final BasicDBObject match;

    public BasicMatchBuilder(AggregationPipline pipline) {
        this.pipline = pipline;
        this.match = new BasicDBObject();
    }

    @Override
    public MatchBuilder where(String fieldName, Object value) {
        match.append(fieldName, value);
        return this;
    }

    @Override
    public MatchBuilder and(String fieldName, Object value) {
        match.append(fieldName, value);
        return this;
    }

    @Override
    public MatchBuilder where(String fieldName, Map<String, Object> critiera) {
        match.append(fieldName, critiera);
        return this;
    }

    @Override
    public MatchBuilder and(String fieldName, Map<String, Object> critiera) {
        match.append(fieldName, critiera);
        return this;
    }

    @Override
    public AggregationPipline create() {
        this.pipline.addOp(new BasicDBObject("$match", match));
        return this.pipline;
    }
}
