package com.aug3.storage.mongoclient.aggregate.builder;


import com.aug3.storage.mongoclient.aggregate.AggregationPipline;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public interface GroupBuilder {

    public GroupBuilder by(String[]... fieldsName);


    public GroupBuilder pushToAs(String fromPiplineField, String toGroupField);

    public GroupBuilder addToSetAs(String fromPiplineField, String toFieldSet);

    public GroupBuilder avgAs(String fromPiplineField, String toAvgField);

    public GroupBuilder firstOf(String fromPiplineField, String toGroupField);

    public GroupBuilder lastOf(String fromPiplineField, String toGroupField);

    public GroupBuilder maxOf(String fromPiplineField, String toGroudField);

    public GroupBuilder minOf(String fromPiplineField, String toGroupField);

    public GroupBuilder countWithName(String toGroupField);


    public AggregationPipline create();


}
