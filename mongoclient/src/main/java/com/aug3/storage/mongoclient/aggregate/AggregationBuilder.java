package com.aug3.storage.mongoclient.aggregate;

import com.mongodb.DBCollection;

/**
 * @author: falcon.chu
 * @date: 13-12-12
 */
public class AggregationBuilder {


    public static AggregationPipline createPiplineOn(DBCollection collection) {
        return new AggregationPipline(collection);
    }


    /**
     * Example of aggregationBuilder
     *
     *
       public static void main(String[] args) {
            DB db = MongoAdaptor.getDB("ada");

            DBCollection targetCollection = db.getCollection("x_funcs");

            AggregationBuilder.createPiplineOn(targetCollection)
                    .group()
                        .by(
                                new String[]{"typ", "$typ"},
                                new String[]{"name", "$name"},
                                new String[]{"cat", "$cat"},
                                new String[]{"catalog", "$catalog"},
                                new String[]{"desc", "$desc"},
                                new String[]{"field", "$field"},
                                new String[]{"tb", "$tb"},
                                new String[]{"order", "$order"},
                                new String[]{"sample", "$sample"}
                        )
                        .countWithName("typeSum")
                        .addToSetAs("$params", "params")
                        .create()
                    .project()
                        .hide("_id")
                        .alias("typ", "$_id.typ")
                        .alias("name", "$_id.name")
                        .alias("desc", "$_id.desc")
                        .alias("sample", "$_id.sample")
                        .alias("catalog", "$_id.catalog")
                        .alias("order", "$_id.order")
                        .alias("cat", "$_id.cat")
                        .alias("required", "$_id.required")
                        .alias("tb", "$_id.tb")
                        .alias("field", "$_id.field")
                        .field("params")
                        .create()
                    .aggregateToCollction(targetCollection);

       }

     *
     *
     */

}
