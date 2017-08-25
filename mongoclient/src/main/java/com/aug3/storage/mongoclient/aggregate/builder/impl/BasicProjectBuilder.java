package com.aug3.storage.mongoclient.aggregate.builder.impl;

import com.aug3.storage.mongoclient.aggregate.AggregationPipline;
import com.aug3.storage.mongoclient.aggregate.builder.ProjectBuilder;
import com.mongodb.BasicDBObject;

import java.util.Map;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public class BasicProjectBuilder implements ProjectBuilder {


    private final AggregationPipline pipline;
    private final BasicDBObject project;

    public BasicProjectBuilder(AggregationPipline pipline) {
        this.pipline = pipline;
        this.project = new BasicDBObject();
    }

    @Override
    public ProjectBuilder field(String fieldName) {
        project.append(fieldName, 1);
        return this;
    }

    @Override
    public ProjectBuilder field(String projectField, Map<String, Object> netestField) {
        project.append(projectField, netestField);
        return this;
    }


    @Override
    public ProjectBuilder alias(String projectField, String originField) {
        project.append(projectField, originField);
        return this;
    }

    @Override
    public ProjectBuilder hide(String fieldName) {
        project.append(fieldName, 0);
        return this;
    }

    @Override
    public AggregationPipline create() {
        this.pipline.addOp(new BasicDBObject("$project", project));
        return this.pipline;
    }
}
