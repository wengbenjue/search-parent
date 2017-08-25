package com.aug3.storage.redisclient;

import java.util.Properties;

/**
 * properties文件加载类
 * 
 * @author jimmy.zhou
 *
 */
public class RedisPropLoader {

	private final String REDIS_CONFIG_FILE = "/redis.properties";

	public final static String REDIS_SERVERS = "redis.servers";
	
	public final static String REDIS_POOL_MAX_ACTIVE = "redis.pool.maxactive";
	public final static String REDIS_POOL_MAX_IDLE = "redis.pool.maxidle";
	public final static String REDIS_POOL_MAX_WAIT = "redis.pool.maxwait";
	public final static String REDIS_POOL_TIME_OUT = "redis.pool.time.out";
	public final static String REDIS_TEST_ON_BORROW = "redis.test.on.borrow";
	public final static String REDIS_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS = "redis.pool.min.evictable.idle.time.millis";
	
	private Properties props;
	
	public RedisPropLoader(){
		try {
			props = new Properties();
			props.load(RedisPropLoader.class.getResourceAsStream(REDIS_CONFIG_FILE));
			
		} catch (Exception e) {
			e.printStackTrace();
			props = null;
		}
	}
	
	public String getProperty(String key, String defaultValue) {
		String value = props.getProperty(key);
		if(value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	public String getProperty(String key, String configKey, String defaultValue) {
		String value = props.getProperty(configKey + "." + key);
		if(value == null) {
			value = props.getProperty(key + "." + configKey);
		}
		if(value == null) {
			value = props.getProperty(configKey);
		}
		if(value == null) {
			value = defaultValue;
		}
		return value;
	}
	
	public int getIntProperty(String key, String configKey, int defaultValue) {
		String value = props.getProperty(key + "." + configKey);
		if(value == null || value.length() == 0) {
			value = props.getProperty(configKey);
		}
		if(value == null || value.length() == 0) {
			return defaultValue;
		}
		return Integer.parseInt(value);
	}
	
	public boolean getBooleanProperty(String key, String configKey, boolean defaultValue) {
		String value = props.getProperty(key + "." + configKey);
		if(value == null || value.length() == 0) {
			value = props.getProperty(configKey);
		}
		if(value == null || value.length() == 0) {
			return defaultValue;
		}
		return Boolean.valueOf(value);
	}
	

}
