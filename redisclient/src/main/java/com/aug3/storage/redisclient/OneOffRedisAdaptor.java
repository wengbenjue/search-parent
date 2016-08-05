package com.aug3.storage.redisclient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.BitPosParams;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

/**
 * 一次性的client
 * 对于单机模式，任何一次读写操作之后，都会执行redis.close()
 * 对于集群模式，等同于RedisAdaptor。因为任意集群操作之后，本身就会归还连接
 * 
 * @author jimmy.zhou
 *
 */
public class OneOffRedisAdaptor extends RedisAdaptor {
	
	/**
	 * 工厂模式的适配器入口
	 * 
	 * @param key 参见redis.properties中的server_name
	 * @return
	 */
	public static OneOffRedisAdaptor getClient(String key){
		return new OneOffRedisAdaptor(key);
	}
	
	public static OneOffRedisAdaptor getClient(){
		return getClient("default");
	}
	
	protected OneOffRedisAdaptor(String key) {
		super(key);
	}
	
	/**
	 * 单机模式需要返还连接给连接池
	 * 该方法取代之前的returnResource方法
	 */
	public void close(){
		super.close();
		super.client = null;
	}
	
	public PipelineAdaptor pipelined(){
		return super.pipelined();
	}
	


	@Override
	public String set(String key, String value) {
		String res = null;
		try {
			res = super.set(key, value);
		} catch (Exception e) {
		} finally {
			super.close();
		}
		return res;
	}
	

	@Override
	public String set(String key, String value, String nxxx, String expx, long time) {
		String res = null;
		try {
			res = super.set(key, value, nxxx, expx, time);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Deprecated
	@Override
	public String set(String key, String value, String nxxx) {
		String res = null;
		try {
			res = super.set(key, value, nxxx);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String get(String key) {
		String res = null;
		try {
			res = super.get(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Boolean exists(String key) {
		Boolean res = null;
		try {
			res = super.exists(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long persist(String key) {
		Long res = null;
		try {
			res = super.persist(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String type(String key) {
		String res = null;
		try {
			res = super.type(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long expire(String key, int seconds) {
		Long res = null;
		try {
			res = super.expire(key, seconds);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long pexpire(String key, long milliseconds) {
		Long res = null;
		try {
			res = super.pexpire(key, milliseconds);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long expireAt(String key, long unixTime) {
		Long res = null;
		try {
			res = super.expireAt(key, unixTime);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long pexpireAt(String key, long millisecondsTimestamp) {
		Long res = null;
		try {
			res = super.pexpireAt(key, millisecondsTimestamp);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long ttl(String key) {
		Long res = null;
		try {
			res = super.ttl(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long pttl(String key) {
		Long res = null;
		try {
			res = super.pttl(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Boolean setbit(String key, long offset, boolean value) {
		Boolean res = null;
		try {
			res = super.setbit(key, offset, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Boolean setbit(String key, long offset, String value) {
		Boolean res = null;
		try {
			res = super.setbit(key, offset, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Boolean getbit(String key, long offset) {
		Boolean res = null;
		try {
			res = super.getbit(key, offset);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long setrange(String key, long offset, String value) {
		Long res = null;
		try {
			res = super.setrange(key, offset, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String getrange(String key, long startOffset, long endOffset) {
		String res = null;
		try {
			res = super.getrange(key, startOffset, endOffset);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String getSet(String key, String value) {
		String res = null;
		try {
			res = super.getSet(key, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long setnx(String key, String value) {
		Long res = null;
		try {
			res = super.setnx(key, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String setex(String key, int seconds, String value) {
		String res = null;
		try {
			res = super.setex(key, seconds, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String psetex(String key, long milliseconds, String value) {
		String res = null;
		try {
			res = super.psetex(key, milliseconds, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long decrBy(String key, long integer) {
		Long res = null;
		try {
			res = super.decrBy(key, integer);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long decr(String key) {
		Long res = null;
		try {
			res = super.decr(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long incrBy(String key, long integer) {
		Long res = null;
		try {
			res = super.incrBy(key, integer);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Double incrByFloat(String key, double value) {
		Double res = null;
		try {
			res = super.incrByFloat(key, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long incr(String key) {
		Long res = null;
		try {
			res = super.incr(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long append(String key, String value) {
		Long res = null;
		try {
			res = super.append(key, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String substr(String key, int start, int end) {
		String res = null;
		try {
			res = super.substr(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long hset(String key, String field, String value) {
		Long res = null;
		try {
			res = super.hset(key, field, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String hget(String key, String field) {
		String res = null;
		try {
			res = super.hget(key, field);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long hsetnx(String key, String field, String value) {
		Long res = null;
		try {
			res = super.hsetnx(key, field, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String hmset(String key, Map<String, String> hash) {
		String res = null;
		try {
			res = super.hmset(key, hash);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> hmget(String key, String... fields) {
		List<String> res = null;
		try {
			res = super.hmget(key, fields);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long hincrBy(String key, String field, long value) {
		Long res = null;
		try {
			res = super.hincrBy(key, field, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Double hincrByFloat(String key, String field, double value) {
		Double res = null;
		try {
			res = super.hincrByFloat(key, field, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Boolean hexists(String key, String field) {
		Boolean res = null;
		try {
			res = super.hexists(key, field);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long hdel(String key, String... field) {
		Long res = null;
		try {
			res = super.hdel(key, field);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long hlen(String key) {
		Long res = null;
		try {
			res = super.hlen(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> hkeys(String key) {
		Set<String> res = null;
		try {
			res = super.hkeys(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> hvals(String key) {
		List<String> res = null;
		try {
			res = super.hvals(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Map<String, String> hgetAll(String key) {
		Map<String, String> res = null;
		try {
			res = super.hgetAll(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long rpush(String key, String... string) {
		Long res = null;
		try {
			res = super.rpush(key, string);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long lpush(String key, String... string) {
		Long res = null;
		try {
			res = super.lpush(key, string);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long llen(String key) {
		Long res = null;
		try {
			res = super.llen(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> lrange(String key, long start, long end) {
		List<String> res = null;
		try {
			res = super.lrange(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String ltrim(String key, long start, long end) {
		String res = null;
		try {
			res = super.ltrim(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String lindex(String key, long index) {
		String res = null;
		try {
			res = super.lindex(key, index);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String lset(String key, long index, String value) {
		String res = null;
		try {
			res = super.lset(key, index, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long lrem(String key, long count, String value) {
		Long res = null;
		try {
			res = super.lrem(key, count, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String lpop(String key) {
		String res = null;
		try {
			res = super.lpop(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String rpop(String key) {
		String res = null;
		try {
			res = super.rpop(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long sadd(String key, String... member) {
		Long res = null;
		try {
			res = super.sadd(key, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> smembers(String key) {
		Set<String> res = null;
		try {
			res = super.smembers(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long srem(String key, String... member) {
		Long res = null;
		try {
			res = super.srem(key, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String spop(String key) {
		String res = null;
		try {
			res = super.spop(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> spop(String key, long count) {
		Set<String> res = null;
		try {
			res = super.spop(key, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long scard(String key) {
		Long res = null;
		try {
			res = super.scard(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Boolean sismember(String key, String member) {
		Boolean res = null;
		try {
			res = super.sismember(key, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String srandmember(String key) {
		String res = null;
		try {
			res = super.srandmember(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> srandmember(String key, int count) {
		List<String> res = null;
		try {
			res = super.srandmember(key, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long strlen(String key) {
		Long res = null;
		try {
			res = super.strlen(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zadd(String key, double score, String member) {
		Long res = null;
		try {
			res = super.zadd(key, score, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zadd(String key, double score, String member, ZAddParams params) {
		Long res = null;
		try {
			res = super.zadd(key, score, member, params);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zadd(String key, Map<String, Double> scoreMembers) {
		Long res = null;
		try {
			res = super.zadd(key, scoreMembers);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
		Long res = null;
		try {
			res = super.zadd(key, scoreMembers, params);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrange(String key, long start, long end) {
		Set<String> res = null;
		try {
			res = super.zrange(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zrem(String key, String... member) {
		Long res = null;
		try {
			res = super.zrem(key, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Double zincrby(String key, double score, String member) {
		Double res = null;
		try {
			res = super.zincrby(key, score, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Double zincrby(String key, double score, String member, ZIncrByParams params) {
		Double res = null;
		try {
			res = super.zincrby(key, score, member, params);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zrank(String key, String member) {
		Long res = null;
		try {
			res = super.zrank(key, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zrevrank(String key, String member) {
		Long res = null;
		try {
			res = super.zrevrank(key, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrevrange(String key, long start, long end) {
		Set<String> res = null;
		try {
			res = super.zrevrange(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrangeWithScores(String key, long start, long end) {
		Set<Tuple> res = null;
		try {
			res = super.zrangeWithScores(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrevrangeWithScores(String key, long start, long end) {
		Set<Tuple> res = null;
		try {
			res = super.zrevrangeWithScores(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zcard(String key) {
		Long res = null;
		try {
			res = super.zcard(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Double zscore(String key, String member) {
		Double res = null;
		try {
			res = super.zscore(key, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> sort(String key) {
		List<String> res = null;
		try {
			res = super.sort(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> sort(String key, SortingParams sortingParameters) {
		List<String> res = null;
		try {
			res = super.sort(key, sortingParameters);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zcount(String key, double min, double max) {
		Long res = null;
		try {
			res = super.zcount(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zcount(String key, String min, String max) {
		Long res = null;
		try {
			res = super.zcount(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrangeByScore(String key, double min, double max) {
		Set<String> res = null;
		try {
			res = super.zrangeByScore(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrangeByScore(String key, String min, String max) {
		Set<String> res = null;
		try {
			res = super.zrangeByScore(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrevrangeByScore(String key, double max, double min) {
		Set<String> res = null;
		try {
			res = super.zrevrangeByScore(key, max, min);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrangeByScore(String key, double min, double max, int offset, int count) {
		Set<String> res = null;
		try {
			res = super.zrangeByScore(key, min, max, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrevrangeByScore(String key, String max, String min) {
		Set<String> res = null;
		try {
			res = super.zrevrangeByScore(key, max, min);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrangeByScore(String key, String min, String max, int offset, int count) {
		Set<String> res = null;
		try {
			res = super.zrangeByScore(key, min, max, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count) {
		Set<String> res = null;
		try {
			res = super.zrevrangeByScore(key, max, min, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
		Set<Tuple> res = null;
		try {
			res = super.zrangeByScoreWithScores(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min) {
		Set<Tuple> res = null;
		try {
			res = super.zrevrangeByScoreWithScores(key, max, min);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
		Set<Tuple> res = null;
		try {
			res = super.zrangeByScoreWithScores(key, min, max, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count) {
		Set<String> res = null;
		try {
			res = super.zrevrangeByScore(key, max, min, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max) {
		Set<Tuple> res = null;
		try {
			res = super.zrangeByScoreWithScores(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min) {
		Set<Tuple> res = null;
		try {
			res = super.zrevrangeByScoreWithScores(key, max, min);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
		Set<Tuple> res = null;
		try {
			res = super.zrangeByScoreWithScores(key, min, max, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
		Set<Tuple> res = null;
		try {
			res = super.zrevrangeByScoreWithScores(key, max, min, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
		Set<Tuple> res = null;
		try {
			res = super.zrevrangeByScoreWithScores(key, max, min, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zremrangeByRank(String key, long start, long end) {
		Long res = null;
		try {
			res = super.zremrangeByRank(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zremrangeByScore(String key, double start, double end) {
		Long res = null;
		try {
			res = super.zremrangeByScore(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zremrangeByScore(String key, String start, String end) {
		Long res = null;
		try {
			res = super.zremrangeByScore(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zlexcount(String key, String min, String max) {
		Long res = null;
		try {
			res = super.zlexcount(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrangeByLex(String key, String min, String max) {
		Set<String> res = null;
		try {
			res = super.zrangeByLex(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrangeByLex(String key, String min, String max, int offset, int count) {
		Set<String> res = null;
		try {
			res = super.zrangeByLex(key, min, max, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrevrangeByLex(String key, String max, String min) {
		Set<String> res = null;
		try {
			res = super.zrevrangeByLex(key, max, min);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count) {
		Set<String> res = null;
		try {
			res = super.zrevrangeByLex(key, max, min, offset, count);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long zremrangeByLex(String key, String min, String max) {
		Long res = null;
		try {
			res = super.zremrangeByLex(key, min, max);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long linsert(String key, LIST_POSITION where, String pivot, String value) {
		Long res = null;
		try {
			res = super.linsert(key, where, pivot, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long lpushx(String key, String... string) {
		Long res = null;
		try {
			res = super.lpushx(key, string);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long rpushx(String key, String... string) {
		Long res = null;
		try {
			res = super.rpushx(key, string);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Deprecated
	@Override
	public List<String> blpop(String arg) {
		List<String> res = null;
		try {
			res = super.blpop(arg);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> blpop(int timeout, String key) {
		List<String> res = null;
		try {
			res = super.blpop(timeout, key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Deprecated
	@Override
	public List<String> brpop(String arg) {
		List<String> res = null;
		try {
			res = super.brpop(arg);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> brpop(int timeout, String key) {
		List<String> res = null;
		try {
			res = super.brpop(timeout, key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long del(String key) {
		Long res = null;
		try {
			res = super.del(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public String echo(String string) {
		String res = null;
		try {
			res = super.echo(string);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Deprecated
	@Override
	public Long move(String key, int dbIndex) {
		Long res = null;
		try {
			res = super.move(key, dbIndex);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long bitcount(String key) {
		Long res = null;
		try {
			res = super.bitcount(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long bitcount(String key, long start, long end) {
		Long res = null;
		try {
			res = super.bitcount(key, start, end);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long bitpos(String key, boolean value) {
		Long res = null;
		try {
			res = super.bitpos(key, value);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long bitpos(String key, boolean value, BitPosParams params) {
		Long res = null;
		try {
			res = super.bitpos(key, value, params);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Deprecated
	@Override
	public ScanResult<Entry<String, String>> hscan(String key, int cursor) {
		ScanResult<Entry<String, String>> res = null;
		try {
			res = super.hscan(key, cursor);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Deprecated
	@Override
	public ScanResult<String> sscan(String key, int cursor) {
		ScanResult<String> res = null;
		try {
			res = super.sscan(key, cursor);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Deprecated
	@Override
	public ScanResult<Tuple> zscan(String key, int cursor) {
		ScanResult<Tuple> res = null;
		try {
			res = super.zscan(key, cursor);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public ScanResult<Entry<String, String>> hscan(String key, String cursor) {
		ScanResult<Entry<String, String>> res = null;
		try {
			res = super.hscan(key, cursor);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public ScanResult<Entry<String, String>> hscan(String key, String cursor, ScanParams params) {
		ScanResult<Entry<String, String>> res = null;
		try {
			res = super.hscan(key, cursor, params);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public ScanResult<String> sscan(String key, String cursor) {
		ScanResult<String> res = null;
		try {
			res = super.sscan(key, cursor);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public ScanResult<String> sscan(String key, String cursor, ScanParams params) {
		ScanResult<String> res = null;
		try {
			res = super.sscan(key, cursor, params);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public ScanResult<Tuple> zscan(String key, String cursor) {
		ScanResult<Tuple> res = null;
		try {
			res = super.zscan(key, cursor);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public ScanResult<Tuple> zscan(String key, String cursor, ScanParams params) {
		ScanResult<Tuple> res = null;
		try {
			res = super.zscan(key, cursor, params);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long pfadd(String key, String... elements) {
		Long res = null;
		try {
			res = super.pfadd(key, elements);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public long pfcount(String key) {
		long res = 0;
		try {
			res = super.pfcount(key);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long geoadd(String key, double longitude, double latitude, String member) {
		Long res = null;
		try {
			res = super.geoadd(key, longitude, latitude, member);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
		Long res = null;
		try {
			res = super.geoadd(key, memberCoordinateMap);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Double geodist(String key, String member1, String member2) {
		Double res = null;
		try {
			res = super.geodist(key, member1, member2);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public Double geodist(String key, String member1, String member2, GeoUnit unit) {
		Double res = null;
		try {
			res = super.geodist(key, member1, member2, unit);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<String> geohash(String key, String... members) {
		List<String> res = null;
		try {
			res = super.geohash(key, members);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<GeoCoordinate> geopos(String key, String... members) {
		List<GeoCoordinate> res = null;
		try {
			res = super.geopos(key, members);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
			GeoUnit unit) {
		List<GeoRadiusResponse> res = null;
		try {
			res = super.georadius(key, longitude, latitude, radius, unit);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit,
			GeoRadiusParam param) {
		List<GeoRadiusResponse> res = null;
		try {
			res = super.georadius(key, longitude, latitude, radius, unit, param);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
		List<GeoRadiusResponse> res = null;
		try {
			res = super.georadiusByMember(key, member, radius, unit);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	@Override
	public List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit,
			GeoRadiusParam param) {
		List<GeoRadiusResponse> res = null;
		try {
			res = super.georadiusByMember(key, member, radius, unit, param);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}

	

	/**
	 * 对于大量节点的集群，可能存在性能问题。不建议广泛使用。
	 * @param pattern
	 * @return
	 */
	@Override
	public Set<String> keys(String pattern) {
		Set<String> res = null;
		try {
			res = super.keys(pattern);
		} catch(Exception e) {
		} finally {
			super.close();
		}
		return res;
	}
	
	/**
	 * 订阅
	 * @param jedisPubSub
	 * @param channels
	 */
	@Override
	public void subscribe(JedisPubSub jedisPubSub, final String... channels){
		super.subscribe(jedisPubSub, channels);
	}
	
	
	
}
