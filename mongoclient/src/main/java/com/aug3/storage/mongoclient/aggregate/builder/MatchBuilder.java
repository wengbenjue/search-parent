package com.aug3.storage.mongoclient.aggregate.builder;


import com.aug3.storage.mongoclient.aggregate.AggregationPipline;

import java.util.Map;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public interface MatchBuilder {

    public MatchBuilder where(String fieldName, Object value);

    public MatchBuilder and(String fieldName, Object value);

    public MatchBuilder where(String fieldName, Map<String, Object> critiera);

    public MatchBuilder and(String fieldName, Map<String, Object> critiera);

    public AggregationPipline create();


}
