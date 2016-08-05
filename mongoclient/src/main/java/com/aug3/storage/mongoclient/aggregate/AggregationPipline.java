package com.aug3.storage.mongoclient.aggregate;

import com.aug3.storage.mongoclient.aggregate.builder.GroupBuilder;
import com.aug3.storage.mongoclient.aggregate.builder.MatchBuilder;
import com.aug3.storage.mongoclient.aggregate.builder.ProjectBuilder;
import com.aug3.storage.mongoclient.aggregate.builder.SortBuilder;
import com.aug3.storage.mongoclient.aggregate.builder.impl.BasicGroupBuilder;
import com.aug3.storage.mongoclient.aggregate.builder.impl.BasicMatchBuilder;
import com.aug3.storage.mongoclient.aggregate.builder.impl.BasicProjectBuilder;
import com.aug3.storage.mongoclient.aggregate.builder.impl.BasicSortBuilder;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public class AggregationPipline {

    private final DBCollection collection;
    public List<DBObject> pipLineOps = new ArrayList<DBObject>();


    public AggregationPipline(DBCollection collection) {
        this.collection = collection;
    }

    public void aggregateToCollction(DBCollection targetCollection) {
        if (getOps().length > 0) {
            DBObject[] sub = Arrays.copyOfRange(getOps(), 1, getOps().length);
            AggregationOutput aggregateOutput = null;
            if (sub.length == 0) {

                aggregateOutput = collection.aggregate(getOps()[0]);
            } else {
                aggregateOutput = collection.aggregate(getOps()[0], sub);
            }

            Iterable<DBObject> result = aggregateOutput.results();
            Iterator<DBObject> iter = result.iterator();
            List<DBObject> targetCollRecord = new ArrayList<DBObject>();
            while (iter.hasNext()) {
                targetCollRecord.add(iter.next());
            }

            targetCollection.insert(targetCollRecord);
        }
    }

    public ProjectBuilder project() {
        return new BasicProjectBuilder(this);
    }

    public MatchBuilder match() {
        return new BasicMatchBuilder(this);
    }

    public GroupBuilder group() {
        return new BasicGroupBuilder(this);
    }

    public SortBuilder sort() {
        return new BasicSortBuilder(this);
    }

    public AggregationPipline skip(Number skip) {
        BasicDBObject skipObj = new BasicDBObject("$skip", skip);
        addOp(skipObj);
        return this;
    }

    public AggregationPipline limit(Number limit) {
        BasicDBObject limitObj = new BasicDBObject("$limit", limit);
        addOp(limitObj);
        return this;
    }

    public AggregationPipline unwind(String unwindField) {
        BasicDBObject unwindObj = new BasicDBObject("$unwind", unwindField);
        addOp(unwindObj);
        return this;
    }


    public void addOp(DBObject op) {
        pipLineOps.add(op);
    }

    public DBObject[] getOps() {

        DBObject[] dbobjArray = new DBObject[pipLineOps.size()];
        for (int i = 0; i < pipLineOps.size(); i++) {
            dbobjArray[i] = pipLineOps.get(i);
        }
        return dbobjArray;
    }

}
