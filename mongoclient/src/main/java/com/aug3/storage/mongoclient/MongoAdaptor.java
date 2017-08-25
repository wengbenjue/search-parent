package com.aug3.storage.mongoclient;

import java.util.HashMap;
import java.util.Map;

import com.aug3.storage.mongoclient.config.MongoConfig;
import com.aug3.storage.mongoclient.exception.BadConfigException;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;

public class MongoAdaptor {
	
	private static MongoConfig config = new MongoConfig();
	
	/**
	 * A database connection with internal connection pooling. For most
	 * applications, you should have one Mongo instance for the entire JVM.
	 */
	private static Map<String, Mongo> mongos = new HashMap<String, Mongo>();
	
	
	/**
	 * Creates a Mongo based on a list of replica set members or a list of
	 * mongos. If you have a standalone server, it will use the
	 * Mongo(ServerAddress) constructor.
	 * 
	 * @return Mongo A database connection with internal connection pooling. For
	 *         most applications, you should have one Mongo instance for the
	 *         entire JVM.
	 * 
	 * @throws BadConfigException
	 */
	public synchronized static Mongo newMongoInstance(String dbName) {
		Mongo instance = mongos.get(dbName);
		if(instance == null) {
			instance = new MongoFactory(dbName).newMongoInstance();
			mongos.put(dbName, instance);
		}
		return instance;
	}
	
	/**
	 * get collection from mongo client
	 * @param collectionName	such as : "ada.base_stock" or "base_stock"
	 * @return
	 */
	public static DBCollection getCollection(String collectionName) {
		String dbKey = null;
		String collName = null;
		
		if(collectionName.indexOf(".") < 0) {
			dbKey = CollectionsRef.coll2dbMap.get(collectionName);
			collName = collectionName;
		} else {
			dbKey = collectionName.split("\\.")[0];
			collName = collectionName.split("\\.")[1];
		}
		
		String dbName = getDbNameByKey(dbKey);
		return newMongoInstance(dbKey).getDB(dbName).getCollection(collName);
	}
	
	/**
	 * get collection from mongo client
	 * @param collectionName	such as : "ada.base_stock"
	 * @return
	 */
	public static DBCollection getDBCollection(String collectionName) {
		String dbKey = collectionName.split("\\.")[0];
		String collName = collectionName.split("\\.")[1];
		String dbName = getDbNameByKey(dbKey);
		return newMongoInstance(dbKey).getDB(dbName).getCollection(collName);
	}

	
    /**
     * get mongo db's name by propery key
     * @param key	such as : "vsto" or "ada" or "fin"
     * @return
     */
    public static String getDbNameByKey(String key) {
    	String res = config.getProperty(key + ".mongo.db.name");
    	if(res == null || res.length() == 0) {
    		res = "ada";
    	}
    	return res;
    }
    
}
