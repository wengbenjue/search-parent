package com.aug3.storage.mongoclient.aggregate.builder.impl;

import com.aug3.storage.mongoclient.aggregate.AggregationPipline;
import com.aug3.storage.mongoclient.aggregate.builder.GroupBuilder;
import com.mongodb.BasicDBObject;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public class BasicGroupBuilder implements GroupBuilder {


    private final AggregationPipline pipline;
    private final BasicDBObject group;

    public BasicGroupBuilder(AggregationPipline pipline) {
        this.pipline = pipline;
        this.group = new BasicDBObject();
    }

    @Override
    public GroupBuilder by(String[]... fieldsName) {
        if (fieldsName.length == 0) {
            throw new IllegalArgumentException("Group Id cannot be empty");
        }
        BasicDBObject groupId = new BasicDBObject();
        for (String[] groupIdPair : fieldsName) {
            if (groupIdPair.length == 1) {

                groupId.append(groupIdPair[0], groupIdPair[0]);
            } else if (groupIdPair.length > 1) {

                groupId.append(groupIdPair[0], groupIdPair[1]);
            } else {
                throw new IllegalArgumentException("Group Id cannot be empty");
            }
        }
        group.append("_id", groupId);
        return this;
    }

    @Override
    public GroupBuilder pushToAs(String fromPiplineField, String toGroupField) {
        group.append(toGroupField, new BasicDBObject("$push", fromPiplineField));
        return this;
    }

    @Override
    public GroupBuilder addToSetAs(String fromPiplineField, String toFieldSet) {
        group.append(toFieldSet, new BasicDBObject("$addToSet", fromPiplineField));
        return this;
    }

    @Override
    public GroupBuilder avgAs(String fromPiplineField, String toAvgField) {
        group.append(toAvgField, new BasicDBObject("$avg", fromPiplineField));
        return this;
    }

    @Override
    public GroupBuilder firstOf(String fromPiplineField, String toGroupField) {
        group.append(toGroupField, new BasicDBObject("$first", fromPiplineField));
        return this;
    }

    @Override
    public GroupBuilder lastOf(String fromPiplineField, String toGroupField) {
        group.append(toGroupField, new BasicDBObject("$last", fromPiplineField));
        return this;
    }

    @Override
    public GroupBuilder maxOf(String fromPiplineField, String toGroudField) {
        group.append(toGroudField, new BasicDBObject("$max", fromPiplineField));
        return this;
    }

    @Override
    public GroupBuilder minOf(String fromPiplineField, String toGroupField) {
        group.append(toGroupField, new BasicDBObject("$min", fromPiplineField));
        return this;
    }

    @Override
    public GroupBuilder countWithName(String toGroupField) {
        group.append(toGroupField, new BasicDBObject("$sum", 1));
        return this;
    }

    @Override
    public AggregationPipline create() {
        this.pipline.addOp(new BasicDBObject("$group", group));
        return this.pipline;
    }
}
