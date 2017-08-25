package com.aug3.storage.redisclient;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;


/**
 * @author Roger.xia
 * @deprecated Use CacheManager instead. 2015-07-08
 */
public class CacheAdaptor {

	private static final Logger logger = Logger.getLogger(CacheAdaptor.class);

	public final static boolean cacheEnabled = 
			Boolean.valueOf((new RedisPropLoader()).getProperty("system.cache.redis", "true"));
	
	public final static int SECONDS_HOUR = 3600;

	public final static int SECONDS_DAY = 3600 * 24;

	public final static int SECONDS_MONTH = 3600 * 24 * 30;

	public static RedisAdaptor getRedisAdaptor() {
		return OneOffRedisAdaptor.getClient();
	}
	
	public static RedisAdaptor getRedisAdaptor(String server_identify) {
		return OneOffRedisAdaptor.getClient(server_identify);
	}

	public static String get(String key) {
		return cacheEnabled ? getRedisAdaptor().get(key) : null;
	}

	public static boolean exist(String key) {
		if (cacheEnabled) {
			try {
				return getRedisAdaptor().exists(key);
			} catch (Exception e) {
			}
		}
		return false;
	}

	public static Set<String> keys(String... keys) {
		if (cacheEnabled) {
			try {
				Set<String> res = new HashSet<String>();
				for(String key : keys) {
					res.addAll(getRedisAdaptor().keys(key));
				}
				return res;
			} catch (Exception e) {
			}
		}
		return Collections.emptySet();
	}

	public static long delete(final String... key) {
		if (cacheEnabled) {
			try {
				return getRedisAdaptor().del(key);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
		return 0;
	}

	public static long deleteByServer(String server_identify, final String... key) {
		if (cacheEnabled) {
			try {
				return getRedisAdaptor(server_identify).del(key);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
		return 0;
	}

	/**
	 * delete fields in hashmap, if fields is null, then delete the whole key.
	 * 
	 * @param key
	 * @param fields
	 */
	public static long hashdelByServer(String server_identify, String key, String... fields) {
		long updated = 0;
		if (cacheEnabled && key != null && key.length() > 0) {
			boolean exists = getRedisAdaptor(server_identify).exists(key);
			if (exists) {
				if (fields != null && fields.length > 0) {
					updated = getRedisAdaptor(server_identify).hdel(key, fields);
				} else {
					updated = getRedisAdaptor(server_identify).del(key);
				}
			}
		}
		return updated;
	}

	/**
	 * This method fetch object from cache;
	 * 
	 * Attention: the current object is serialized using google-gson
	 * 
	 * @param key
	 * @param obj
	 * @return
	 */
	public static Object getObject(String key, Class<?> obj) {
		if (cacheEnabled) {
			try {
				return getRedisAdaptor().getObject(key, obj);
			} catch (Exception e) {
			}
		}
		return null;
	}

	/**
	 * This method fetch object from cache;
	 * 
	 * Attention: the current object is serialized using java serialization
	 * 
	 * @param key
	 * @return
	 */
	public static Object getSerialObject(String key) {
		if (cacheEnabled) {
			try {
				return getRedisAdaptor().getSerializableObj(key);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return null;
	}

	public static void set(String key, String value) {
		if (cacheEnabled) {
			try {
				getRedisAdaptor().set(key, value);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
	}

	public static void setex(String key, String value, int expireseconds) {
		if (cacheEnabled) {
			try {
				getRedisAdaptor().setex(key, expireseconds, value);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
	}

	public static void setexObject(String key, Object value, int expiredSeconds) {
		if (cacheEnabled) {
			try {
				getRedisAdaptor().setexObject(key, value, expiredSeconds);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
	}

	/**
	 * This method fetch object from cache;
	 * 
	 * Attention: the current object is serialized using java serialization,
	 * make sure your object implement Serializable interface.
	 * 
	 * @param key
	 * @param value
	 * @param expireseconds
	 */
	public static void setexSerialObject(String key, Object value, int expireseconds) {
		if (cacheEnabled) {
			try {
				getRedisAdaptor().setexSerializableObj(key, value, expireseconds);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
	}

	/**
	 * EXPIREAT works exctly like EXPIRE but instead to get the number of
	 * seconds representing the Time To Live of the key as a second argument
	 * (that is a relative way of specifing the TTL), it takes an absolute one
	 * in the form of a UNIX timestamp (Number of seconds elapsed since 1 Jan
	 * 1970).
	 * 
	 * @param key
	 * @param unixTime
	 */
	public static void expireAt(String key, long unixTime) {
		if (cacheEnabled) {
			try {
				getRedisAdaptor().expireAt(key, unixTime);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}
		}
	}

}
