package com.aug3.storage.redisclient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.aug3.storage.redisclient.util.ObjectResolver;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import redis.clients.extend.JedisClusterExt;
import redis.clients.extend.PipelineClusterExt;
import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

/**
 * redis客户端适配类
 * 同时支持单机/集群模式
 * 
 * @author jimmy.zhou
 *
 */
public class RedisAdaptor implements JedisCommands {
	
	JedisCommands client = null;
	String key = null;
	
	/**
	 * 工厂模式的适配器入口
	 * 
	 * @param key 参见redis.properties中的server_name
	 * @return
	 */
	public static RedisAdaptor getClient(String key){
		return new RedisAdaptor(key);
	}
	
	public static RedisAdaptor getClient(){
		return getClient("default");
	}
	
	protected RedisAdaptor(String key) {
		this.client = RedisPoolMgr.getInstance().getClient(key);
		this.key = key;
	}
	
	protected JedisCommands getCli(){
		if(this.client == null) {
			this.client = RedisPoolMgr.getInstance().getClient(key);
		}
		return this.client;
	}
	
	public boolean isSingleMode(){
		return client instanceof Jedis;
	}
	
	/**
	 * 单机模式需要返还连接给连接池
	 * 该方法取代之前的returnResource方法
	 */
	public void close(){
		if(isSingleMode()) {
			((Jedis)client).close();
		}
	}
	
	public PipelineAdaptor pipelined(){
		if(isSingleMode()) {
			Pipeline pipelined = ((Jedis) getCli()).pipelined();
			return new PipelineAdaptor(pipelined);
		} else {
			PipelineClusterExt pipelined = ((JedisClusterExt) getCli()).pipelined();
			return new PipelineAdaptor(pipelined);
		}
	}

	@Override
	public String set(String key, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).set(key, value);
		} else {
			return ((JedisClusterExt) getCli()).set(key, value);
		}
	}

	@Override
	public String set(String key, String value, String nxxx, String expx, long time) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).set(key, value, nxxx, expx, time);
		} else {
			return ((JedisClusterExt) getCli()).set(key, value, nxxx, expx, time);
		}
	}

	@Deprecated
	@Override
	public String set(String key, String value, String nxxx) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).set(key, value, nxxx);
		} else {
			return ((JedisClusterExt) getCli()).set(key, value, nxxx);
		}
	}

	@Override
	public String get(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).get(key);
		} else {
			return ((JedisClusterExt) getCli()).get(key);
		}
	}

	@Override
	public Boolean exists(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).exists(key);
		} else {
			return ((JedisClusterExt) getCli()).exists(key);
		}
	}

	@Override
	public Long persist(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).persist(key);
		} else {
			return ((JedisClusterExt) getCli()).persist(key);
		}
	}

	@Override
	public String type(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).type(key);
		} else {
			return ((JedisClusterExt) getCli()).type(key);
		}
	}

	@Override
	public Long expire(String key, int seconds) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).expire(key, seconds);
		} else {
			return ((JedisClusterExt) getCli()).expire(key, seconds);
		}
	}

	@Override
	public Long pexpire(String key, long milliseconds) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).pexpire(key, milliseconds);
		} else {
			return ((JedisClusterExt) getCli()).pexpire(key, milliseconds);
		}
	}

	@Override
	public Long expireAt(String key, long unixTime) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).expireAt(key, unixTime);
		} else {
			return ((JedisClusterExt) getCli()).expireAt(key, unixTime);
		}
	}

	@Override
	public Long pexpireAt(String key, long millisecondsTimestamp) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).pexpireAt(key, millisecondsTimestamp);
		} else {
			return ((JedisClusterExt) getCli()).pexpireAt(key, millisecondsTimestamp);
		}
	}

	@Override
	public Long ttl(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).ttl(key);
		} else {
			return ((JedisClusterExt) getCli()).ttl(key);
		}
	}

	@Override
	public Long pttl(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).pttl(key);
		} else {
			return ((JedisClusterExt) getCli()).pttl(key);
		}
	}

	@Override
	public Boolean setbit(String key, long offset, boolean value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).setbit(key, offset, value);
		} else {
			return ((JedisClusterExt) getCli()).setbit(key, offset, value);
		}
	}

	@Override
	public Boolean setbit(String key, long offset, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).setbit(key, offset, value);
		} else {
			return ((JedisClusterExt) getCli()).setbit(key, offset, value);
		}
	}

	@Override
	public Boolean getbit(String key, long offset) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).getbit(key, offset);
		} else {
			return ((JedisClusterExt) getCli()).getbit(key, offset);
		}
	}

	@Override
	public Long setrange(String key, long offset, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).setrange(key, offset, value);
		} else {
			return ((JedisClusterExt) getCli()).setrange(key, offset, value);
		}
	}

	@Override
	public String getrange(String key, long startOffset, long endOffset) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).getrange(key, startOffset, endOffset);
		} else {
			return ((JedisClusterExt) getCli()).getrange(key, startOffset, endOffset);
		}
	}

	@Override
	public String getSet(String key, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).getSet(key, value);
		} else {
			return ((JedisClusterExt) getCli()).getSet(key, value);
		}
	}

	@Override
	public Long setnx(String key, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).setnx(key, value);
		} else {
			return ((JedisClusterExt) getCli()).setnx(key, value);
		}
	}

	@Override
	public String setex(String key, int seconds, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).setex(key, seconds, value);
		} else {
			return ((JedisClusterExt) getCli()).setex(key, seconds, value);
		}
	}

	@Override
	public String psetex(String key, long milliseconds, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).psetex(key, milliseconds, value);
		} else {
			return ((JedisClusterExt) getCli()).psetex(key, milliseconds, value);
		}
	}

	@Override
	public Long decrBy(String key, long integer) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).decrBy(key, integer);
		} else {
			return ((JedisClusterExt) getCli()).decrBy(key, integer);
		}
	}

	@Override
	public Long decr(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).decr(key);
		} else {
			return ((JedisClusterExt) getCli()).decr(key);
		}
	}

	@Override
	public Long incrBy(String key, long integer) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).incrBy(key, integer);
		} else {
			return ((JedisClusterExt) getCli()).incrBy(key, integer);
		}
	}

	@Override
	public Double incrByFloat(String key, double value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).incrByFloat(key, value);
		} else {
			return ((JedisClusterExt) getCli()).incrByFloat(key, value);
		}
	}

	@Override
	public Long incr(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).incr(key);
		} else {
			return ((JedisClusterExt) getCli()).incr(key);
		}
	}

	@Override
	public Long append(String key, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).append(key, value);
		} else {
			return ((JedisClusterExt) getCli()).append(key, value);
		}
	}

	@Override
	public String substr(String key, int start, int end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).substr(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).substr(key, start, end);
		}
	}

	@Override
	public Long hset(String key, String field, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hset(key, field, value);
		} else {
			return ((JedisClusterExt) getCli()).hset(key, field, value);
		}
	}

	@Override
	public String hget(String key, String field) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hget(key, field);
		} else {
			return ((JedisClusterExt) getCli()).hget(key, field);
		}
	}

	@Override
	public Long hsetnx(String key, String field, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hsetnx(key, field, value);
		} else {
			return ((JedisClusterExt) getCli()).hsetnx(key, field, value);
		}
	}

	@Override
	public String hmset(String key, Map<String, String> hash) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hmset(key, hash);
		} else {
			return ((JedisClusterExt) getCli()).hmset(key, hash);
		}
	}

	@Override
	public List<String> hmget(String key, String... fields) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hmget(key, fields);
		} else {
			return ((JedisClusterExt) getCli()).hmget(key, fields);
		}
	}

	@Override
	public Long hincrBy(String key, String field, long value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hincrBy(key, field, value);
		} else {
			return ((JedisClusterExt) getCli()).hincrBy(key, field, value);
		}
	}

	@Override
	public Double hincrByFloat(String key, String field, double value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hincrByFloat(key, field, value);
		} else {
			return ((JedisClusterExt) getCli()).hincrByFloat(key, field, value);
		}
	}

	@Override
	public Boolean hexists(String key, String field) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hexists(key, field);
		} else {
			return ((JedisClusterExt) getCli()).hexists(key, field);
		}
	}

	@Override
	public Long hdel(String key, String... field) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hdel(key, field);
		} else {
			return ((JedisClusterExt) getCli()).hdel(key, field);
		}
	}

	@Override
	public Long hlen(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hlen(key);
		} else {
			return ((JedisClusterExt) getCli()).hlen(key);
		}
	}

	@Override
	public Set<String> hkeys(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hkeys(key);
		} else {
			return ((JedisClusterExt) getCli()).hkeys(key);
		}
	}

	@Override
	public List<String> hvals(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hvals(key);
		} else {
			return ((JedisClusterExt) getCli()).hvals(key);
		}
	}

	@Override
	public Map<String, String> hgetAll(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hgetAll(key);
		} else {
			return ((JedisClusterExt) getCli()).hgetAll(key);
		}
	}

	@Override
	public Long rpush(String key, String... string) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).rpush(key, string);
		} else {
			return ((JedisClusterExt) getCli()).rpush(key, string);
		}
	}

	@Override
	public Long lpush(String key, String... string) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).lpush(key, string);
		} else {
			return ((JedisClusterExt) getCli()).lpush(key, string);
		}
	}

	@Override
	public Long llen(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).llen(key);
		} else {
			return ((JedisClusterExt) getCli()).llen(key);
		}
	}

	@Override
	public List<String> lrange(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).lrange(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).lrange(key, start, end);
		}
	}

	@Override
	public String ltrim(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).ltrim(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).ltrim(key, start, end);
		}
	}

	@Override
	public String lindex(String key, long index) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).lindex(key, index);
		} else {
			return ((JedisClusterExt) getCli()).lindex(key, index);
		}
	}

	@Override
	public String lset(String key, long index, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).lset(key, index, value);
		} else {
			return ((JedisClusterExt) getCli()).lset(key, index, value);
		}
	}

	@Override
	public Long lrem(String key, long count, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).lrem(key, count, value);
		} else {
			return ((JedisClusterExt) getCli()).lrem(key, count, value);
		}
	}

	@Override
	public String lpop(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).lpop(key);
		} else {
			return ((JedisClusterExt) getCli()).lpop(key);
		}
	}

	@Override
	public String rpop(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).rpop(key);
		} else {
			return ((JedisClusterExt) getCli()).rpop(key);
		}
	}

	@Override
	public Long sadd(String key, String... member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).sadd(key, member);
		} else {
			return ((JedisClusterExt) getCli()).sadd(key, member);
		}
	}

	@Override
	public Set<String> smembers(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).smembers(key);
		} else {
			return ((JedisClusterExt) getCli()).smembers(key);
		}
	}

	@Override
	public Long srem(String key, String... member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).srem(key, member);
		} else {
			return ((JedisClusterExt) getCli()).srem(key, member);
		}
	}

	@Override
	public String spop(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).spop(key);
		} else {
			return ((JedisClusterExt) getCli()).spop(key);
		}
	}

	@Override
	public Set<String> spop(String key, long count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).spop(key, count);
		} else {
			return ((JedisClusterExt) getCli()).spop(key, count);
		}
	}

	@Override
	public Long scard(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).scard(key);
		} else {
			return ((JedisClusterExt) getCli()).scard(key);
		}
	}

	@Override
	public Boolean sismember(String key, String member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).sismember(key, member);
		} else {
			return ((JedisClusterExt) getCli()).sismember(key, member);
		}
	}

	@Override
	public String srandmember(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).srandmember(key);
		} else {
			return ((JedisClusterExt) getCli()).srandmember(key);
		}
	}

	@Override
	public List<String> srandmember(String key, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).srandmember(key, count);
		} else {
			return ((JedisClusterExt) getCli()).srandmember(key, count);
		}
	}

	@Override
	public Long strlen(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).strlen(key);
		} else {
			return ((JedisClusterExt) getCli()).strlen(key);
		}
	}

	@Override
	public Long zadd(String key, double score, String member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zadd(key, score, member);
		} else {
			return ((JedisClusterExt) getCli()).zadd(key, score, member);
		}
	}

	@Override
	public Long zadd(String key, double score, String member, ZAddParams params) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zadd(key, score, member, params);
		} else {
			return ((JedisClusterExt) getCli()).zadd(key, score, member, params);
		}
	}

	@Override
	public Long zadd(String key, Map<String, Double> scoreMembers) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zadd(key, scoreMembers);
		} else {
			return ((JedisClusterExt) getCli()).zadd(key, scoreMembers);
		}
	}

	@Override
	public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zadd(key, scoreMembers, params);
		} else {
			return ((JedisClusterExt) getCli()).zadd(key, scoreMembers, params);
		}
	}

	@Override
	public Set<String> zrange(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrange(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).zrange(key, start, end);
		}
	}

	@Override
	public Long zrem(String key, String... member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrem(key, member);
		} else {
			return ((JedisClusterExt) getCli()).zrem(key, member);
		}
	}

	@Override
	public Double zincrby(String key, double score, String member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zincrby(key, score, member);
		} else {
			return ((JedisClusterExt) getCli()).zincrby(key, score, member);
		}
	}

	@Override
	public Double zincrby(String key, double score, String member, ZIncrByParams params) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zincrby(key, score, member, params);
		} else {
			return ((JedisClusterExt) getCli()).zincrby(key, score, member, params);
		}
	}

	@Override
	public Long zrank(String key, String member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrank(key, member);
		} else {
			return ((JedisClusterExt) getCli()).zrank(key, member);
		}
	}

	@Override
	public Long zrevrank(String key, String member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrank(key, member);
		} else {
			return ((JedisClusterExt) getCli()).zrevrank(key, member);
		}
	}

	@Override
	public Set<String> zrevrange(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrange(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).zrevrange(key, start, end);
		}
	}

	@Override
	public Set<Tuple> zrangeWithScores(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeWithScores(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).zrangeWithScores(key, start, end);
		}
	}

	@Override
	public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeWithScores(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeWithScores(key, start, end);
		}
	}

	@Override
	public Long zcard(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zcard(key);
		} else {
			return ((JedisClusterExt) getCli()).zcard(key);
		}
	}

	@Override
	public Double zscore(String key, String member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zscore(key, member);
		} else {
			return ((JedisClusterExt) getCli()).zscore(key, member);
		}
	}

	@Override
	public List<String> sort(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).sort(key);
		} else {
			return ((JedisClusterExt) getCli()).sort(key);
		}
	}

	@Override
	public List<String> sort(String key, SortingParams sortingParameters) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).sort(key, sortingParameters);
		} else {
			return ((JedisClusterExt) getCli()).sort(key, sortingParameters);
		}
	}

	@Override
	public Long zcount(String key, double min, double max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zcount(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zcount(key, min, max);
		}
	}

	@Override
	public Long zcount(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zcount(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zcount(key, min, max);
		}
	}

	@Override
	public Set<String> zrangeByScore(String key, double min, double max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByScore(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByScore(key, min, max);
		}
	}

	@Override
	public Set<String> zrangeByScore(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByScore(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByScore(key, min, max);
		}
	}

	@Override
	public Set<String> zrevrangeByScore(String key, double max, double min) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByScore(key, max, min);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByScore(key, max, min);
		}
	}

	@Override
	public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByScore(key, min, max, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByScore(key, min, max, offset, count);
		}
	}

	@Override
	public Set<String> zrevrangeByScore(String key, String max, String min) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByScore(key, max, min);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByScore(key, max, min);
		}
	}

	@Override
	public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByScore(key, min, max, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByScore(key, min, max, offset, count);
		}
	}

	@Override
	public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByScore(key, max, min, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByScore(key, max, min, offset, count);
		}
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByScoreWithScores(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByScoreWithScores(key, min, max);
		}
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByScoreWithScores(key, max, min);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByScoreWithScores(key, max, min);
		}
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByScoreWithScores(key, min, max, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByScoreWithScores(key, min, max, offset, count);
		}
	}

	@Override
	public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByScore(key, max, min, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByScore(key, max, min, offset, count);
		}
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByScoreWithScores(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByScoreWithScores(key, min, max);
		}
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByScoreWithScores(key, max, min);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByScoreWithScores(key, max, min);
		}
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByScoreWithScores(key, min, max, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByScoreWithScores(key, min, max, offset, count);
		}
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByScoreWithScores(key, max, min, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByScoreWithScores(key, max, min, offset, count);
		}
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByScoreWithScores(key, max, min, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByScoreWithScores(key, max, min, offset, count);
		}
	}

	@Override
	public Long zremrangeByRank(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zremrangeByRank(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).zremrangeByRank(key, start, end);
		}
	}

	@Override
	public Long zremrangeByScore(String key, double start, double end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zremrangeByScore(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).zremrangeByScore(key, start, end);
		}
	}

	@Override
	public Long zremrangeByScore(String key, String start, String end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zremrangeByScore(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).zremrangeByScore(key, start, end);
		}
	}

	@Override
	public Long zlexcount(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zlexcount(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zlexcount(key, min, max);
		}
	}

	@Override
	public Set<String> zrangeByLex(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByLex(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByLex(key, min, max);
		}
	}

	@Override
	public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrangeByLex(key, min, max, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrangeByLex(key, min, max, offset, count);
		}
	}

	@Override
	public Set<String> zrevrangeByLex(String key, String max, String min) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByLex(key, max, min);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByLex(key, max, min);
		}
	}

	@Override
	public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zrevrangeByLex(key, max, min, offset, count);
		} else {
			return ((JedisClusterExt) getCli()).zrevrangeByLex(key, max, min, offset, count);
		}
	}

	@Override
	public Long zremrangeByLex(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zremrangeByLex(key, min, max);
		} else {
			return ((JedisClusterExt) getCli()).zremrangeByLex(key, min, max);
		}
	}

	@Override
	public Long linsert(String key, LIST_POSITION where, String pivot, String value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).linsert(key, where, pivot, value);
		} else {
			return ((JedisClusterExt) getCli()).linsert(key, where, pivot, value);
		}
	}

	@Override
	public Long lpushx(String key, String... string) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).lpushx(key, string);
		} else {
			return ((JedisClusterExt) getCli()).lpushx(key, string);
		}
	}

	@Override
	public Long rpushx(String key, String... string) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).rpushx(key, string);
		} else {
			return ((JedisClusterExt) getCli()).rpushx(key, string);
		}
	}

	@Deprecated
	@Override
	public List<String> blpop(String arg) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).blpop(arg);
		} else {
			return ((JedisClusterExt) getCli()).blpop(arg);
		}
	}

	@Override
	public List<String> blpop(int timeout, String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).blpop(timeout, key);
		} else {
			return ((JedisClusterExt) getCli()).blpop(timeout, key);
		}
	}

	@Deprecated
	@Override
	public List<String> brpop(String arg) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).brpop(arg);
		} else {
			return ((JedisClusterExt) getCli()).brpop(arg);
		}
	}

	@Override
	public List<String> brpop(int timeout, String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).brpop(timeout, key);
		} else {
			return ((JedisClusterExt) getCli()).brpop(timeout, key);
		}
	}

	@Override
	public Long del(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).del(key);
		} else {
			return ((JedisClusterExt) getCli()).del(key);
		}
	}
	
	public Long del(String... keys) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).del(keys);
		} else {
			return ((JedisClusterExt) getCli()).del(keys);
		}
	}
	
	@Override
	public String echo(String string) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).echo(string);
		} else {
			return ((JedisClusterExt) getCli()).echo(string);
		}
	}

	@Deprecated
	@Override
	public Long move(String key, int dbIndex) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).move(key, dbIndex);
		} else {
			return ((JedisClusterExt) getCli()).move(key, dbIndex);
		}
	}

	@Override
	public Long bitcount(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).bitcount(key);
		} else {
			return ((JedisClusterExt) getCli()).bitcount(key);
		}
	}

	@Override
	public Long bitcount(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).bitcount(key, start, end);
		} else {
			return ((JedisClusterExt) getCli()).bitcount(key, start, end);
		}
	}

	@Override
	public Long bitpos(String key, boolean value) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).bitpos(key, value);
		} else {
			return ((JedisClusterExt) getCli()).bitpos(key, value);
		}
	}

	@Override
	public Long bitpos(String key, boolean value, BitPosParams params) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).bitpos(key, value, params);
		} else {
			return ((JedisClusterExt) getCli()).bitpos(key, value, params);
		}
	}

	@Deprecated
	@Override
	public ScanResult<Entry<String, String>> hscan(String key, int cursor) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hscan(key, cursor);
		} else {
			return ((JedisClusterExt) getCli()).hscan(key, cursor);
		}
	}

	@Deprecated
	@Override
	public ScanResult<String> sscan(String key, int cursor) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).sscan(key, cursor);
		} else {
			return ((JedisClusterExt) getCli()).sscan(key, cursor);
		}
	}

	@Deprecated
	@Override
	public ScanResult<Tuple> zscan(String key, int cursor) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zscan(key, cursor);
		} else {
			return ((JedisClusterExt) getCli()).zscan(key, cursor);
		}
	}

	@Override
	public ScanResult<Entry<String, String>> hscan(String key, String cursor) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hscan(key, cursor);
		} else {
			return ((JedisClusterExt) getCli()).hscan(key, cursor);
		}
	}

	@Override
	public ScanResult<Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).hscan(key, cursor, params);
		} else {
			return ((JedisClusterExt) getCli()).hscan(key, cursor, params);
		}
	}

	@Override
	public ScanResult<String> sscan(String key, String cursor) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).sscan(key, cursor);
		} else {
			return ((JedisClusterExt) getCli()).sscan(key, cursor);
		}
	}

	@Override
	public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).sscan(key, cursor, params);
		} else {
			return ((JedisClusterExt) getCli()).sscan(key, cursor, params);
		}
	}

	@Override
	public ScanResult<Tuple> zscan(String key, String cursor) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zscan(key, cursor);
		} else {
			return ((JedisClusterExt) getCli()).zscan(key, cursor);
		}
	}

	@Override
	public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).zscan(key, cursor, params);
		} else {
			return ((JedisClusterExt) getCli()).zscan(key, cursor, params);
		}
	}

	@Override
	public Long pfadd(String key, String... elements) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).pfadd(key, elements);
		} else {
			return ((JedisClusterExt) getCli()).pfadd(key, elements);
		}
	}

	@Override
	public long pfcount(String key) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).pfcount(key);
		} else {
			return ((JedisClusterExt) getCli()).pfcount(key);
		}
	}

	@Override
	public Long geoadd(String key, double longitude, double latitude, String member) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).geoadd(key, longitude, latitude, member);
		} else {
			return ((JedisClusterExt) getCli()).geoadd(key, longitude, latitude, member);
		}
	}

	@Override
	public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).geoadd(key, memberCoordinateMap);
		} else {
			return ((JedisClusterExt) getCli()).geoadd(key, memberCoordinateMap);
		}
	}

	@Override
	public Double geodist(String key, String member1, String member2) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).geodist(key, member1, member2);
		} else {
			return ((JedisClusterExt) getCli()).geodist(key, member1, member2);
		}
	}

	@Override
	public Double geodist(String key, String member1, String member2, GeoUnit unit) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).geodist(key, member1, member2, unit);
		} else {
			return ((JedisClusterExt) getCli()).geodist(key, member1, member2, unit);
		}
	}

	@Override
	public List<String> geohash(String key, String... members) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).geohash(key, members);
		} else {
			return ((JedisClusterExt) getCli()).geohash(key, members);
		}
	}

	@Override
	public List<GeoCoordinate> geopos(String key, String... members) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).geopos(key, members);
		} else {
			return ((JedisClusterExt) getCli()).geopos(key, members);
		}
	}

	@Override
	public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
			GeoUnit unit) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).georadius(key, longitude, latitude, radius, unit);
		} else {
			return ((JedisClusterExt) getCli()).georadius(key, longitude, latitude, radius, unit);
		}
	}

	@Override
	public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit,
			GeoRadiusParam param) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).georadius(key, longitude, latitude, radius, unit, param);
		} else {
			return ((JedisClusterExt) getCli()).georadius(key, longitude, latitude, radius, unit, param);
		}
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).georadiusByMember(key, member, radius, unit);
		} else {
			return ((JedisClusterExt) getCli()).georadiusByMember(key, member, radius, unit);
		}
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit,
			GeoRadiusParam param) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).georadiusByMember(key, member, radius, unit, param);
		} else {
			return ((JedisClusterExt) getCli()).georadiusByMember(key, member, radius, unit, param);
		}
	}
	
	/**
	 * 对于大量节点的集群，可能存在性能问题。不建议广泛使用。
	 * @param pattern
	 * @return
	 */
	public Set<String> keys(String pattern) {
		if(isSingleMode()) {
			return ((Jedis) getCli()).keys(pattern);
		} else {
			return ((JedisClusterExt) getCli()).keys(pattern);
		}
	}
	
	/**
	 * 订阅
	 * @param jedisPubSub
	 * @param channels
	 */
	public void subscribe(JedisPubSub jedisPubSub, final String... channels){
		if(isSingleMode()) {
			((Jedis) getCli()).subscribe(jedisPubSub, channels);
		} else {
			((JedisClusterExt) getCli()).subscribe(jedisPubSub, channels);
		}
	}
	
	/**
	 * 基于对象序列化的  hset
	 * 属于代码迁移产物，不建议使用
	 */
	public long hsetObj(String key, String field, Object obj) {
		try {
			String value = ObjectResolver.encode(obj);
			Long res = this.hset(key, field, value);
			if(res == null) {
				return 0l;
			}
			return res;
		} catch (IOException e) {
			return 0;
		}
	}
	
	/**
	 * 基于对象序列化的 get
	 * 属于代码迁移产物，不建议使用
	 */
	public Object getSerializableObj(String key) throws Exception {
		String value = this.get(key);
		return ObjectResolver.decode(value);
	}
	
	/**
	 *  基于对象序列化的 setex
	 *  属于代码迁移产物，不建议使用
	 */
	public String setexSerializableObj(String key, Object value, int expiredSeconds) throws IOException {
		if (value != null) {
			return this.setex(key, expiredSeconds, ObjectResolver.encode(value));
		}
		return null;
	}
	
	/**
	 * 基于对象序列化的 hget
	 * 属于代码迁移产物，不建议使用
	 */
	public <T> T hgetObj(String key, Class<T> clz, String field) {
		String value = this.hget(key, field);
		T ret = null;
		try {
			ret = (T) ObjectResolver.decode(value);
		} catch (Exception e) {
		}
		return ret;
	}

	/**
	 * 基于对象序列化的 hmset
	 * 属于代码迁移产物，不建议使用
	 */
	public <T> String hmsetObj(String key, Map<String, T> hash) {
		String ret = null;
		Map<String, String> hashStr = new HashMap<String, String>(hash.size());
		try {
			for (Map.Entry<String, T> entry : hash.entrySet()) {
				hashStr.put(entry.getKey(), ObjectResolver.encode(entry.getValue()));
			}
		} catch (IOException e) {
			return ret;
		}
		return this.hmset(key, hashStr);
	}

	/**
	 * 基于对象序列化的hmget
	 * 属于代码迁移产物，不建议使用
	 */
	public <T> Map<String, T> hmgetObj(String key, Class<T> clz, String... fields) {
		List<String> data = this.hmget(key, fields);
		Map<String, T> map = new HashMap<String, T>();
		try {
			for (int i = 0; i < fields.length; i++) {
				if (data.get(i) != null) {
					map.put(fields[i], (T) ObjectResolver.decode(data.get(i)));
				}
			}
		} catch (Exception e) {
		}
		return map;
	}
	
	/**
	 * 旧代码迁移，不得不引入的gson对象
	 */
	protected Gson gson = new GsonBuilder().serializeNulls().create();
	
	/**
	 * 基于gson化的 get
	 * 属于代码迁移产物，不建议使用
	 */
	public Object getObject(String key, Class<?> obj) {
		return gson.fromJson(this.get(key), obj);
	}

	/**
	 * 基于gson化的 get
	 * 属于代码迁移产物，不建议使用
	 */
	public Object getObject(String key, Type genericType) {
		return gson.fromJson(this.get(key), genericType);
	}
	
	/**
	 * 基于gson化的 setex
	 * 属于代码迁移产物，不建议使用
	 */
	public String setexObject(String key, Object value, int expiredSeconds) {
		return this.setex(key, expiredSeconds, gson.toJson(value));
	}
	
	
}
