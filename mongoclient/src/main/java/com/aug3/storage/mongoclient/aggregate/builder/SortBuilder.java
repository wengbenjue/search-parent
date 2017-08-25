package com.aug3.storage.mongoclient.aggregate.builder;


import com.aug3.storage.mongoclient.aggregate.AggregationPipline;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public interface SortBuilder {

    public SortBuilder with(String keyName, Integer direction);

    public AggregationPipline create();



}
