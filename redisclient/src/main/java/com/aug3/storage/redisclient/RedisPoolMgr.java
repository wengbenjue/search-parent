package com.aug3.storage.redisclient;


import java.util.HashSet;
import java.util.Set;

import redis.clients.extend.JedisClusterExt;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * redis连接池创建和管理
 * 
 * @author jimmy.zhou
 *
 */
public class RedisPoolMgr {
	
	private static RedisPoolMgr instance;
	
	private RedisPoolMgr(){
	}
	
	public static RedisPoolMgr getInstance() {
		if(instance == null) {
			instance = new RedisPoolMgr();
		}
		return instance;
	}
	
	public JedisCommands getClient() {
		return getClient("default");
	}
	
	public JedisCommands getClient(String key) {
		Integer poolMode = RedisPool.poolMode.get(key);
		if(poolMode == null) {
			initPool(key);
		}
		poolMode = RedisPool.poolMode.get(key);
		if(RedisPool.POOL_SINGLE_MODE.equals(poolMode)) {
			JedisPool jedisPool = RedisPool.singlePool.get(key);
			return jedisPool.getResource();
		} else if(RedisPool.POOL_CLUSTER_MODE.equals(poolMode)) {
			return RedisPool.clusterPool.get(key);
		}
		return null;
	}
	
	protected JedisPoolConfig initPoolConfig(String key) {
		JedisPoolConfig config = new JedisPoolConfig();
		
		RedisPropLoader loader = new RedisPropLoader();
		config.setMaxTotal(
				loader.getIntProperty(key, RedisPropLoader.REDIS_POOL_MAX_ACTIVE, 20));
		config.setMaxIdle(
				loader.getIntProperty(key, RedisPropLoader.REDIS_POOL_MAX_IDLE, 10));
		config.setMaxWaitMillis(
				loader.getIntProperty(key, RedisPropLoader.REDIS_POOL_MAX_WAIT, 1000));
		config.setTestOnBorrow(
				loader.getBooleanProperty(key, RedisPropLoader.REDIS_TEST_ON_BORROW, true));
		config.setMinEvictableIdleTimeMillis(
				loader.getIntProperty(key, RedisPropLoader.REDIS_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS, 1800000));
		
		return config;
	}
	
	
	protected void initPool(String key) {
		RedisPropLoader loader = new RedisPropLoader();
		String servers = loader.getProperty(key, RedisPropLoader.REDIS_SERVERS, null);
		if (servers == null) {
			throw new RuntimeException("Redis server is not well configured. server selector : redis.servers." + key);
		}
		
		String[] serverArr = servers.split(",");
		if(serverArr.length == 1) {
			/*
			 *  单机环境
			 */
			String[] hostp = serverArr[0].split(":");
			String host = hostp[0];
			int port = hostp.length == 1 ? 6379 : Integer.parseInt(hostp[1]);
			// 请求操作超时时间。即使不配置该参数，jedis默认也是2秒。
			int timeout = loader.getIntProperty(key, RedisPropLoader.REDIS_POOL_TIME_OUT, 2000);
			JedisPool jedisPool = new JedisPool(initPoolConfig(key), host, port, timeout);
			
			RedisPool.singlePool.put(key, jedisPool);
			RedisPool.poolMode.put(key, RedisPool.POOL_SINGLE_MODE);
		} else {
			/*
			 *  集群环境
			 */
			Set<HostAndPort> nodes = new HashSet<HostAndPort>();
			for(String oneServer : serverArr) {
				String[] hostp = oneServer.split(":");
				String host = hostp[0];
				int port = hostp.length == 1 ? 6379 : Integer.parseInt(hostp[1]);
				nodes.add(new HostAndPort(host, port));
			}
			
			// 请求操作超时时间。即使不配置该参数，jedis默认也是2秒。
			int timeout = loader.getIntProperty(key, RedisPropLoader.REDIS_POOL_TIME_OUT, 2000);
			JedisClusterExt jedisClusterExt = new JedisClusterExt(nodes, timeout, initPoolConfig(key));
			
			RedisPool.clusterPool.put(key, jedisClusterExt);
			RedisPool.poolMode.put(key, RedisPool.POOL_CLUSTER_MODE);
		}
		
	}
	
	

}
