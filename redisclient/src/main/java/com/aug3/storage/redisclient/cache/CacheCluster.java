package com.aug3.storage.redisclient.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.aug3.storage.redisclient.OneOffRedisAdaptor;
import com.aug3.storage.redisclient.RedisAdaptor;

/**
 * This class encapsulate methods for distributed cache implemented by redis
 * 
 * @author Roger.xia
 * 
 */
public class CacheCluster {

	public final static RedisAdaptor getCacheAdaptor() {
		return OneOffRedisAdaptor.getClient("cluster");
	}

	/**
	 * get String value by key, if not exists, build the data and store it to
	 * cache.
	 * 
	 * @param key
	 * @param placeholder
	 * @param builder
	 * @return
	 * @throws Exception
	 */
	public static String getAndSet(CacheKey key, String placeholder, AbstractDataBuilder builder) throws Exception {

		String cacheKey = null;

		if (placeholder == null || placeholder.length() == 0) {
			cacheKey = key.getKey();
		} else {
			cacheKey = key.getKey(placeholder);
		}

		String retObj = getCacheAdaptor().get(cacheKey);
		if (retObj != null)
			return retObj;

		retObj = (String) builder.buildData();

		if (retObj != null) {
			getCacheAdaptor().setex(cacheKey, key.getTtl(), retObj);
		}

		return retObj;

	}

	/**
	 * Get Object value by key, if key not exists, build the data from data
	 * source and cache it.
	 * 
	 * Make sure your cached Object should implement Serializable.
	 * 
	 * @param key
	 * @param placeholder
	 * @param builder
	 * @return
	 * @throws Exception
	 */
	public static <T> T getAndSetObj(CacheKey key, String key_placeholder, AbstractDataBuilder<T> builder)
			throws Exception {

		return getAndSetObj(key, key_placeholder, builder, true);

	}
	
	/**
	 * rewrite by function
	 */
	public static <T> T getAndSetObj(CacheKey key, String key_placeholder, Supplier<T> supplier)
			throws Exception {
		return getAndSetObj(key, key_placeholder, supplier, true);
	}
	
	/**
	 * This is a tricky one for getAndSetObj, we add a needCache flag to skip
	 * cache when needed.
	 * 
	 * @param key
	 * @param placeholder
	 * @param builder
	 * @param needCache
	 * @return
	 * @throws Exception
	 */
	public static <T> T getAndSetObj(CacheKey key, String placeholder, AbstractDataBuilder<T> builder, boolean needCache)
			throws Exception {

		if (needCache) {
			String cacheKey = null;

			if (placeholder == null || placeholder.length() == 0) {
				cacheKey = key.getKey();
			} else {
				cacheKey = key.getKey(placeholder);
			}

			T retObj = (T) getCacheAdaptor().getSerializableObj(cacheKey);

			if (retObj != null)
				return retObj;

			retObj = builder.buildData();

			if (retObj != null) {
				getCacheAdaptor().setexSerializableObj(cacheKey, retObj, key.getTtl());
			}
			return retObj;
		} else {
			return builder.buildData();
		}

	}
	
	/**
	 * rewrite by function
	 */
	public static <T> T getAndSetObj(CacheKey key, String placeholder, Supplier<T> supplier, boolean needCache)
			throws Exception {
		if (needCache) {
			String cacheKey = null;

			if (placeholder == null || placeholder.length() == 0) {
				cacheKey = key.getKey();
			} else {
				cacheKey = key.getKey(placeholder);
			}

			T retObj = (T) getCacheAdaptor().getSerializableObj(cacheKey);

			if (retObj != null)
				return retObj;

			retObj = supplier.get();

			if (retObj != null) {
				getCacheAdaptor().setexSerializableObj(cacheKey, retObj, key.getTtl());
			}
			return retObj;
		} else {
			return supplier.get();
		}
	}
	
	/**
	 * get String value from hash : {cache_key:{field:string_value}}, if not
	 * exists, find it in data store and cached it.
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param field
	 * @param builder
	 * @return
	 * @throws Exception
	 */
	public static String hashGetAndSet(CacheKey key, String key_placeholder, String field,
			AbstractDataBuilder<String> builder) throws Exception {

		String cacheKey = key.getKey(key_placeholder);

		boolean exist = getCacheAdaptor().exists(cacheKey);

		String value = null;

		if (exist) {
			value = getCacheAdaptor().hget(cacheKey, field);

			if (value != null)
				return value;

		}

		value = builder.buildData();

		if (value != null) {
			getCacheAdaptor().hset(cacheKey, field, value);
			if (!exist)
				getCacheAdaptor().expire(cacheKey, key.getTtl());
		}
		return value;

	}

	/**
	 * get Class<T> type value from hash : {cache_key:{field:Class<T>}}, if not
	 * exists, find it in data store and cached it.
	 * 
	 * Make sure <T> implements Serializable.
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param field
	 * @param clz
	 * @param builder
	 * @return
	 * @throws Exception
	 */
	public static <T> T hashGetAndSetObj(CacheKey key, String key_placeholder, String field, Class<T> clz,
			AbstractDataBuilder<T> builder) throws Exception {

		String cacheKey = key.getKey(key_placeholder);

		boolean exist = getCacheAdaptor().exists(cacheKey);

		T value = null;

		if (exist) {
			value = getCacheAdaptor().hgetObj(cacheKey, clz, field);

			if (value != null)
				return value;

		}

		value = builder.buildData();

		if (value != null) {
			getCacheAdaptor().hsetObj(cacheKey, field, value);
			if (!exist)
				getCacheAdaptor().expire(cacheKey, key.getTtl());
		}
		return value;

	}

	/**
	 * get multiple object value for specified keys from cache :
	 * {cache_key:{field:Object}}, if not exists, find it in data store and
	 * cached it.
	 * 
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param fields
	 * @param typeOfValue
	 * @param builder
	 * @return Map : only the values found from cache and data store
	 * @throws Exception
	 */
	public static <T> Map<String, T> hashMGetAndSetObj(CacheKey key, String key_placeholder,
			final Collection<String> fields, Class<T> typeOfValue, HashDataBuilder<Map<String, T>> builder)
			throws Exception {

		String cacheKey = key.getKey(key_placeholder);

		boolean exist = getCacheAdaptor().exists(cacheKey);

		Map<String, T> resultMap = new HashMap<String, T>();
		Set<String> left = new HashSet<String>(fields);
		if (exist) {
			resultMap.putAll(getCacheAdaptor().hmgetObj(cacheKey, typeOfValue, left.toArray(new String[] {})));
			if (!resultMap.isEmpty()) {
				if (resultMap.size() == left.size()) {
					return resultMap;
				}
				left.removeAll(resultMap.keySet());
			}
		}

		Map<String, T> valueMap = builder.buildData(left);

		if (valueMap != null && !valueMap.isEmpty()) {
			resultMap.putAll(valueMap);
			getCacheAdaptor().hmsetObj(cacheKey, valueMap);
			if (!exist)
				getCacheAdaptor().expire(cacheKey, key.getTtl());
		}
		return resultMap;

	}

	/**
	 * get type <T> object value from hash by field: {cache_key:{field:type <T>
	 * Object}}
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param field
	 * @param clz
	 * @return
	 * @throws Exception
	 */
	public static <T> T hgetObj(CacheKey key, String key_placeholder, String field, Class<T> clz) throws Exception {

		String cacheKey = key.getKey(key_placeholder);
		if (getCacheAdaptor().exists(cacheKey)) {
			return getCacheAdaptor().hgetObj(cacheKey, clz, field);
		}
		return null;

	}

	/**
	 * Set type object value to hash for field: {cache_key:{field:Object}}
	 * 
	 * Make sure your object should implement Serializable.
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param field
	 * @param valueObj
	 * @throws Exception
	 */
	public static void hsetObj(CacheKey key, String key_placeholder, String field, Object valueObj) throws Exception {

		if (valueObj != null) {
			String cacheKey = key.getKey(key_placeholder);
			boolean exist = getCacheAdaptor().exists(cacheKey);
			getCacheAdaptor().hsetObj(cacheKey, field, valueObj);
			if (!exist)
				getCacheAdaptor().expire(cacheKey, key.getTtl());
		}

	}

	/**
	 * get multiple type <T> object value from hash by fields:
	 * {cache_key:{field:type <T> Object}}
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param fields
	 * @param clz
	 * @return
	 * @throws Exception
	 */
	public static <T> Map<String, T> hmgetObj(CacheKey key, String key_placeholder, String[] fields, Class<T> clz)
			throws Exception {

		String cacheKey = key.getKey(key_placeholder);
		if (getCacheAdaptor().exists(cacheKey)) {
			return getCacheAdaptor().hmgetObj(cacheKey, clz, fields);
		}
		return null;

	}

	/**
	 * Set HashMap<String, ? extends Serializable> object to hash :
	 * {cache_key:{field:Object}}
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param hash
	 * @throws Exception
	 */
	public static void hmsetObj(CacheKey key, String key_placeholder, Map hash) throws Exception {

		if (hash != null) {
			String cacheKey = key.getKey(key_placeholder);
			boolean exist = getCacheAdaptor().exists(cacheKey);
			getCacheAdaptor().hmsetObj(cacheKey, hash);
			if (!exist)
				getCacheAdaptor().expire(cacheKey, key.getTtl());
		}

	}

	/**
	 * get multiple String object value from hash by fields:
	 * {cache_key:{field:String}}
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param fields
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> hmget(CacheKey key, String key_placeholder, String[] fields) throws Exception {

		String cacheKey = key.getKey(key_placeholder);

		boolean exist = getCacheAdaptor().exists(cacheKey);

		Map<String, String> hash = new HashMap<String, String>();
		List<String> list = null;
		if (exist) {
			list = getCacheAdaptor().hmget(cacheKey, fields);
			if (list != null) {
				for (int i = 0; i < fields.length; i++) {
					if (list.get(i) != null)
						hash.put(fields[i], list.get(i));
				}
			}
		}
		return hash;
	}

	/**
	 * Set HashMap<String, String> object to hash : {cache_key:{field:String}}
	 * 
	 * @param key
	 * @param key_placeholder
	 * @param hash
	 * @throws Exception
	 */
	public static void hmset(CacheKey key, String key_placeholder, Map<String, String> hash) throws Exception {

		if (hash != null) {
			String cacheKey = key.getKey(key_placeholder);
			boolean exist = getCacheAdaptor().exists(cacheKey);
			getCacheAdaptor().hmset(cacheKey, hash);
			if (!exist)
				getCacheAdaptor().expire(cacheKey, key.getTtl());
		}

	}

	/**
	 * delete fields in hashmap, if fields is null, then delete the whole key.
	 * 
	 * @param key
	 * @param fields
	 */
	public static void hashdel(String key, String... fields) {

		if (key == null) {
			return;
		}

		boolean exists = getCacheAdaptor().exists(key);
		if (exists) {
			if (fields != null && fields.length > 0) {
				getCacheAdaptor().hdel(key, fields);
			} else {
				getCacheAdaptor().del(key);
			}
		}

	}

	/**
	 * flush all cache key in pattern specified by key parameter.
	 * 
	 * @param key
	 * @return
	 */
	public static int flush(String key) {

		if (key == null) {
			return 0;
		}
		Set<String> keys = null;
		int numCleared = 0;
		if ("all".equalsIgnoreCase(key)) {
			keys = getCacheAdaptor().keys("*");
		} else {
			keys = new HashSet<String>();
			String[] keyArray = key.split(",");
			for (String k : keyArray) {
				keys.addAll(getCacheAdaptor().keys(k));
			}
		}
		numCleared += getCacheAdaptor().del(keys.toArray(new String[0]));
		return numCleared;

	}

	/**
	 * get all cache keys in pattern specified by key parameter.
	 * 
	 * @param key
	 * @return
	 */
	public static Set<String> getKeys(String key) {

		Set<String> keys = null;
		if (key == null || "all".equalsIgnoreCase(key)) {
			keys = getCacheAdaptor().keys("*");
		} else {
			keys = new HashSet<String>();
			String[] keyArray = key.split(",");
			for (String k : keyArray) {
				keys.addAll(getCacheAdaptor().keys(k));
			}
		}
		return keys;

	}
	
	/**
	 * delete multiple keys
	 * @param keys
	 */
	public static void del(String... keys) {
		if (keys == null) {
			return;
		}
		for (String key : keys) {
			if (getCacheAdaptor().exists(key)) {
				getCacheAdaptor().del(key);
			}
		}
	}

}
