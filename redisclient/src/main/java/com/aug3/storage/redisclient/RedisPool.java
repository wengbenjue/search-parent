package com.aug3.storage.redisclient;

import java.util.HashMap;
import java.util.Map;

import redis.clients.extend.JedisClusterExt;
import redis.clients.jedis.JedisPool;

/**
 * redis连接池
 * 同时兼容单机模式和集群模式
 * 
 * @author jimmy.zhou
 *
 */
public class RedisPool {

	public static Integer POOL_SINGLE_MODE = 1;	// 单机模式
	public static Integer POOL_CLUSTER_MODE = 2; // 集群模式
	
	// 集群模式连接池
	public static Map<String, JedisClusterExt> clusterPool = new HashMap<String, JedisClusterExt>();
	// 单机模式连接池
	public static Map<String, JedisPool> singlePool = new HashMap<String, JedisPool>();
	// 单机/集群 模式字典
	public static Map<String, Integer> poolMode = new HashMap<String, Integer>();
	
}
