package com.aug3.storage.mongoclient.aggregate.builder.impl;

import com.aug3.storage.mongoclient.aggregate.AggregationPipline;
import com.aug3.storage.mongoclient.aggregate.builder.SortBuilder;
import com.mongodb.BasicDBObject;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public class BasicSortBuilder implements SortBuilder {

    private final AggregationPipline pipline;

    private final BasicDBObject sort;

    public BasicSortBuilder(AggregationPipline pipline) {
        this.pipline = pipline;
        this.sort = new BasicDBObject();
    }

    @Override
    public SortBuilder with(String keyName, Integer direction) {
        sort.append(keyName, direction);
        return this;
    }

    @Override
    public AggregationPipline create() {
        this.pipline.addOp(new BasicDBObject("$sort", sort));
        return this.pipline;
    }

}
