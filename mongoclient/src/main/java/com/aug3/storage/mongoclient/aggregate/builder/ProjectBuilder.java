package com.aug3.storage.mongoclient.aggregate.builder;


import com.aug3.storage.mongoclient.aggregate.AggregationPipline;

import java.util.Map;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public interface ProjectBuilder {

    public ProjectBuilder field(String fieldName);

    public ProjectBuilder field(String projectField, Map<String, Object> netestField);

    public ProjectBuilder alias(String projectField, String originField);

    public ProjectBuilder hide(String fieldName);

    public AggregationPipline create();
}
