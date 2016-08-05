package com.aug3.storage.mongoclient;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class MongoUtils {
	
	public static synchronized List<Long> nextval(String name, int count) {
		List<Long> result = new ArrayList<Long>();
		try {
			DBCollection collection = MongoAdaptor.getCollection("ids");
			BasicDBObject query = new BasicDBObject("name", name);
			DBObject oldValueObj = collection.findOne(query);
			long oldValue = oldValueObj == null ? 1 : Long.valueOf(oldValueObj.get("value").toString().replace(".0", ""));
			BasicDBObject update = new BasicDBObject("$inc", new BasicDBObject("value", (long) count));
			long newValue = Long.valueOf(collection.findAndModify(query, null, null, false, update, true, true)
					.get("value").toString().replace(".0", ""));

			for (long i = oldValue == 1 ? 1 : oldValue + 1; i <= newValue; i++) {
				result.add(i);
			}
		} catch (MongoException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static synchronized long nextval(String name) {
		long value = 0;
		try {
			BasicDBObject query = new BasicDBObject("name", name);
			BasicDBObject update = new BasicDBObject("$inc", new BasicDBObject("value", 1l));
			return Long.valueOf(MongoAdaptor.getCollection("ids").findAndModify(query, null, null, false, update, true, true)
					.get("value").toString().replace(".0", ""));
		} catch (MongoException e) {
			e.printStackTrace();
		}
		return value;
	}

	public static Operator translate(String operator) {
		operator = operator.trim();

		if (operator.equals("=") || operator.equals("=="))
			return Operator.EQUAL;
		else if (operator.equals(">"))
			return Operator.GREATER_THAN;
		else if (operator.equals(">="))
			return Operator.GREATER_THAN_OR_EQUAL;
		else if (operator.equals("<"))
			return Operator.LESS_THAN;
		else if (operator.equals("<="))
			return Operator.LESS_THAN_OR_EQUAL;
		else if (operator.equals("!=") || operator.equals("<>"))
			return Operator.NOT_EQUAL;
		else if (operator.toLowerCase().equals("in"))
			return Operator.IN;
		else if (operator.toLowerCase().equals("nin"))
			return Operator.NOT_IN;
		else if (operator.toLowerCase().equals("all"))
			return Operator.ALL;
		else if (operator.toLowerCase().equals("exists"))
			return Operator.EXISTS;
		else if (operator.toLowerCase().equals("elem"))
			return Operator.ELEMENT_MATCH;
		else if (operator.toLowerCase().equals("size"))
			return Operator.SIZE;
		else if (operator.toLowerCase().equals("within"))
			return Operator.WITHIN;
		else if (operator.toLowerCase().equals("near"))
			return Operator.NEAR;
		else
			throw new IllegalArgumentException("Unknown operator '" + operator + "'");
	}

	public static void insert(DBCollection collection, LinkedHashMap<String, Object> map) throws MongoException {

		DBObject dbObj = new BasicDBObject(map);
		collection.insert(dbObj);
	}

	public static void save(DBCollection collection, LinkedHashMap<String, Object> map) throws MongoException {

		BasicDBObject doc = new BasicDBObject(map);

		collection.save(doc);
	}

	public static boolean update(DBCollection collection, LinkedHashMap<String, Object> queryMap,
			LinkedHashMap<String, Object> updateMap, boolean multi) {

		BasicDBObject query = prepareDBObject(queryMap);

		BasicDBObject updateFields = prepareDBObject(updateMap);

		BasicDBObject update = new BasicDBObject("$set", updateFields);

		if (multi) {
			collection.updateMulti(query, update);
		} else {
			collection.update(query, update);
		}
		return true;
	}

	private static BasicDBObject prepareDBObject(LinkedHashMap<String, Object> map) {
		BasicDBObject dbObj = new BasicDBObject();

		Set<String> s = map.keySet();
		for (String key : s) {
			dbObj.put(key, map.get(key));
		}

		return dbObj;
	}

	/**
	 * return filed value as String
	 * use getObjectByFieldNameChain if value is not String type
	 */
    public static String getStringByFieldNameChain(DBObject obj, String filedNameChain) {
        Object returnObj = getObjectByFieldNameChain(obj, filedNameChain);
        if (returnObj != null) {
            return returnObj.toString();
        } else {
            return null;
        }
    }
	   
	/**
	 * get value according to fied name as name.szh
	 * 
	 * @param obj
	 * @param filedNameChain
	 * @return
	 */

	public static Object getObjectByFieldNameChain(DBObject obj, String filedNameChain) {
		if (obj == null || filedNameChain == null || filedNameChain.length() == 0) {
			return null;
		}
		return getObjectByFieldNameChain(obj, filedNameChain.split("\\."));
	}

	public static Object getObjectByFieldNameChain(DBObject obj, String[] fieldNames) {
		if (obj == null || fieldNames.length == 0) {
			return null;
		}
		if (fieldNames.length == 1) {
			return obj.get(fieldNames[0]);
		}

		for (String fieldName : fieldNames) {
			Object childObj = obj.get(fieldName);
			if (childObj instanceof DBObject) {
				return getObjectByFieldNameChain((DBObject) childObj, (String[]) removeItemFromArray(fieldNames, 0));
			}
		}
		return null;
	}

	/**
	 * get one field from list
	 * 
	 * @param list
	 * @param field
	 * @return
	 */
	public static List<String> fetchFieldsFromList(List<DBObject> list, String field) {
		List<String> rList = new ArrayList<String>();
		if (null != list && !list.isEmpty()) {
			for (DBObject dbo : list) {
				Object value = getObjectByFieldNameChain(dbo, field);
				if (value != null) {
					rList.add(value.toString());
				}
			}
		}
		return rList;
	}

	// remove one item from array
	private static String[] removeItemFromArray(String[] array, int index) {
		int length = array.length;
		if (index < 0 || index >= length) {
			throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
		}

		Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
		System.arraycopy(array, 0, result, 0, index);
		if (index < length - 1) {
			System.arraycopy(array, index + 1, result, index, length - index - 1);
		}
		return (String[]) result;
	}
	
	/**
	 * 初始化 ids中 给定table的自增id, 
     * 成功返回最终更新的条数
     * 失败返回 -1
     * 
	 * @param name 欲初始化的id的表名
	 * @param initialValue 
	 * @return 
	 */
	public static int initialID(String name, long initialValue){
		int updated = 0;
	   	try {
	   		 DBCollection coll = MongoAdaptor.getCollection("ids");
	   		 BasicDBObject query = new BasicDBObject("name", name);
	   		 BasicDBObject update = new BasicDBObject("$set", new BasicDBObject("value", initialValue));
	   		 WriteResult wresult = coll.update(query, update,true,false);
	   		
	   		 /* JAR UPDATE FROM 2.13.1 TO 3.2.2
	   		  * CommandResult commandResult = wresult.getLastError();
	   		 if(commandResult.getLong("n") > 0 && commandResult.getDouble("ok") == 1.0 && commandResult.get("err") == null){
	   			 updated = commandResult.getInt("n");
	   		 }else{
	   			 updated = -1;
	   		 }*/
	   		 
	   		 boolean wasAcknowledged = wresult.wasAcknowledged();
	   		 int n = wresult.getN();
	   		 if (wasAcknowledged && n > 0) {
				updated = n;
			} else {
				updated = -1;
			}
	   		 
	   	 } catch(MongoException e){
	   		 e.printStackTrace();
	   		 updated = -1;
	   	 }
	   	return updated;
	}

}
