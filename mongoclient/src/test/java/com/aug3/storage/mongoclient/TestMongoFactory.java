package com.aug3.storage.mongoclient;

import org.junit.Ignore;
import org.junit.Test;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class TestMongoFactory {

	@Test
	public void test() {

		MongoFactory mf = new MongoFactory("192.168.0.223:27017");
		Mongo m = mf.newMongoInstance();
		DB db = m.getDB("ada");
		int size = db.getCollectionNames().size();
		System.out.println(size);
		
		DBCollection collection = MongoAdaptor.getCollection("ada.base_stock");
		DBObject findOne = collection.findOne();
		System.out.println(findOne);
	}

}
