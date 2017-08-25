package com.aug3.storage.redisclient;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.extend.PipelineClusterExt;
import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.commands.RedisPipeline;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

/**
 * redis客户端pipeline适配类
 * 同时支持单机/集群模式
 * 
 * @author jimmy.zhou
 *
 */
public class PipelineAdaptor implements RedisPipeline {
	
	RedisPipeline redisPipeline = null;
	
	public PipelineAdaptor(RedisPipeline redisPipeline) {
		this.redisPipeline = redisPipeline;
	}
	
	public boolean isSingleMode(){
		return redisPipeline instanceof Pipeline;
	}
	
	public void sync(){
		if(isSingleMode()) {
			((Pipeline) redisPipeline).sync();
		} else {
			((PipelineClusterExt) redisPipeline).sync();
		}
	}

	@Override
	public Response<Long> append(String key, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).append(key, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).append(key, value);
		}
	}
	

	@Override
	public Response<List<String>> blpop(String arg) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).blpop(arg);
		} else {
			return ((PipelineClusterExt) redisPipeline).blpop(arg);
		}
	}

	@Override
	public Response<List<String>> brpop(String arg) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).brpop(arg);
		} else {
			return ((PipelineClusterExt) redisPipeline).brpop(arg);
		}
	}

	@Override
	public Response<Long> decr(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).decr(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).decr(key);
		}
	}

	@Override
	public Response<Long> decrBy(String key, long integer) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).decrBy(key, integer);
		} else {
			return ((PipelineClusterExt) redisPipeline).decrBy(key, integer);
		}
	}

	@Override
	public Response<Long> del(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).del(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).del(key);
		}
	}

	@Override
	public Response<String> echo(String string) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).echo(string);
		} else {
			return ((PipelineClusterExt) redisPipeline).echo(string);
		}
	}

	@Override
	public Response<Boolean> exists(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).exists(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).exists(key);
		}
	}

	@Override
	public Response<Long> expire(String key, int seconds) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).expire(key, seconds);
		} else {
			return ((PipelineClusterExt) redisPipeline).expire(key, seconds);
		}
	}

	@Override
	public Response<Long> pexpire(String key, long milliseconds) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).pexpire(key, milliseconds);
		} else {
			return ((PipelineClusterExt) redisPipeline).pexpire(key, milliseconds);
		}
	}

	@Override
	public Response<Long> expireAt(String key, long unixTime) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).expireAt(key, unixTime);
		} else {
			return ((PipelineClusterExt) redisPipeline).expireAt(key, unixTime);
		}
	}

	@Override
	public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).pexpireAt(key, millisecondsTimestamp);
		} else {
			return ((PipelineClusterExt) redisPipeline).pexpireAt(key, millisecondsTimestamp);
		}
	}

	@Override
	public Response<String> get(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).get(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).get(key);
		}
	}

	@Override
	public Response<Boolean> getbit(String key, long offset) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).getbit(key, offset);
		} else {
			return ((PipelineClusterExt) redisPipeline).getbit(key, offset);
		}
	}

	@Override
	public Response<String> getrange(String key, long startOffset, long endOffset) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).getrange(key, startOffset, endOffset);
		} else {
			return ((PipelineClusterExt) redisPipeline).getrange(key, startOffset, endOffset);
		}
	}

	@Override
	public Response<String> getSet(String key, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).getSet(key, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).getSet(key, value);
		}
	}

	@Override
	public Response<Long> hdel(String key, String... field) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hdel(key, field);
		} else {
			return ((PipelineClusterExt) redisPipeline).hdel(key, field);
		}
	}

	@Override
	public Response<Boolean> hexists(String key, String field) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hexists(key, field);
		} else {
			return ((PipelineClusterExt) redisPipeline).hexists(key, field);
		}
	}

	@Override
	public Response<String> hget(String key, String field) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hget(key, field);
		} else {
			return ((PipelineClusterExt) redisPipeline).hget(key, field);
		}
	}

	@Override
	public Response<Map<String, String>> hgetAll(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hgetAll(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).hgetAll(key);
		}
	}

	@Override
	public Response<Long> hincrBy(String key, String field, long value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hincrBy(key, field, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).hincrBy(key, field, value);
		}
	}

	@Override
	public Response<Set<String>> hkeys(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hkeys(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).hkeys(key);
		}
	}

	@Override
	public Response<Long> hlen(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hlen(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).hlen(key);
		}
	}

	@Override
	public Response<List<String>> hmget(String key, String... fields) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hmget(key, fields);
		} else {
			return ((PipelineClusterExt) redisPipeline).hmget(key, fields);
		}
	}

	@Override
	public Response<String> hmset(String key, Map<String, String> hash) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hmset(key, hash);
		} else {
			return ((PipelineClusterExt) redisPipeline).hmset(key, hash);
		}
	}

	@Override
	public Response<Long> hset(String key, String field, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hset(key, field, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).hset(key, field, value);
		}
	}

	@Override
	public Response<Long> hsetnx(String key, String field, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hsetnx(key, field, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).hsetnx(key, field, value);
		}
	}

	@Override
	public Response<List<String>> hvals(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).hvals(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).hvals(key);
		}
	}

	@Override
	public Response<Long> incr(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).incr(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).incr(key);
		}
	}

	@Override
	public Response<Long> incrBy(String key, long integer) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).incrBy(key, integer);
		} else {
			return ((PipelineClusterExt) redisPipeline).incrBy(key, integer);
		}
	}

	@Override
	public Response<String> lindex(String key, long index) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).lindex(key, index);
		} else {
			return ((PipelineClusterExt) redisPipeline).lindex(key, index);
		}
	}

	@Override
	public Response<Long> linsert(String key, LIST_POSITION where, String pivot, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).linsert(key, where, pivot, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).linsert(key, where, pivot, value);
		}
	}

	@Override
	public Response<Long> llen(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).llen(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).llen(key);
		}
	}

	@Override
	public Response<String> lpop(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).lpop(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).lpop(key);
		}
	}

	@Override
	public Response<Long> lpush(String key, String... string) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).lpush(key, string);
		} else {
			return ((PipelineClusterExt) redisPipeline).lpush(key, string);
		}
	}

	@Override
	public Response<Long> lpushx(String key, String... string) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).lpushx(key, string);
		} else {
			return ((PipelineClusterExt) redisPipeline).lpushx(key, string);
		}
	}

	@Override
	public Response<List<String>> lrange(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).lrange(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).lrange(key, start, end);
		}
	}

	@Override
	public Response<Long> lrem(String key, long count, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).lrem(key, count, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).lrem(key, count, value);
		}
	}

	@Override
	public Response<String> lset(String key, long index, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).lset(key, index, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).lset(key, index, value);
		}
	}

	@Override
	public Response<String> ltrim(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).ltrim(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).ltrim(key, start, end);
		}
	}

	@Override
	public Response<Long> move(String key, int dbIndex) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).move(key, dbIndex);
		} else {
			return ((PipelineClusterExt) redisPipeline).move(key, dbIndex);
		}
	}

	@Override
	public Response<Long> persist(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).persist(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).persist(key);
		}
	}

	@Override
	public Response<String> rpop(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).rpop(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).rpop(key);
		}
	}

	@Override
	public Response<Long> rpush(String key, String... string) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).rpush(key, string);
		} else {
			return ((PipelineClusterExt) redisPipeline).rpush(key, string);
		}
	}

	@Override
	public Response<Long> rpushx(String key, String... string) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).rpushx(key, string);
		} else {
			return ((PipelineClusterExt) redisPipeline).rpushx(key, string);
		}
	}

	@Override
	public Response<Long> sadd(String key, String... member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).sadd(key, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).sadd(key, member);
		}
	}

	@Override
	public Response<Long> scard(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).scard(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).scard(key);
		}
	}

	@Override
	public Response<Boolean> sismember(String key, String member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).sismember(key, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).sismember(key, member);
		}
	}

	@Override
	public Response<String> set(String key, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).set(key, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).set(key, value);
		}
	}

	@Override
	public Response<Boolean> setbit(String key, long offset, boolean value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).setbit(key, offset, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).setbit(key, offset, value);
		}
	}

	@Override
	public Response<String> setex(String key, int seconds, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).setex(key, seconds, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).setex(key, seconds, value);
		}
	}

	@Override
	public Response<Long> setnx(String key, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).setnx(key, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).setnx(key, value);
		}
	}

	@Override
	public Response<Long> setrange(String key, long offset, String value) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).setrange(key, offset, value);
		} else {
			return ((PipelineClusterExt) redisPipeline).setrange(key, offset, value);
		}
	}

	@Override
	public Response<Set<String>> smembers(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).smembers(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).smembers(key);
		}
	}

	@Override
	public Response<List<String>> sort(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).sort(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).sort(key);
		}
	}

	@Override
	public Response<List<String>> sort(String key, SortingParams sortingParameters) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).sort(key, sortingParameters);
		} else {
			return ((PipelineClusterExt) redisPipeline).sort(key, sortingParameters);
		}
	}

	@Override
	public Response<String> spop(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).spop(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).spop(key);
		}
	}

	@Override
	public Response<Set<String>> spop(String key, long count) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).spop(key, count);
		} else {
			return ((PipelineClusterExt) redisPipeline).spop(key, count);
		}
	}

	@Override
	public Response<String> srandmember(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).srandmember(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).srandmember(key);
		}
	}

	@Override
	public Response<Long> srem(String key, String... member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).srem(key, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).srem(key, member);
		}
	}

	@Override
	public Response<Long> strlen(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).strlen(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).strlen(key);
		}
	}

	@Override
	public Response<String> substr(String key, int start, int end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).substr(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).substr(key, start, end);
		}
	}

	@Override
	public Response<Long> ttl(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).ttl(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).ttl(key);
		}
	}

	@Override
	public Response<String> type(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).type(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).type(key);
		}
	}

	@Override
	public Response<Long> zadd(String key, double score, String member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zadd(key, score, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).zadd(key, score, member);
		}
	}

	@Override
	public Response<Long> zadd(String key, double score, String member, ZAddParams params) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zadd(key, score, member, params);
		} else {
			return ((PipelineClusterExt) redisPipeline).zadd(key, score, member, params);
		}
	}

	@Override
	public Response<Long> zadd(String key, Map<String, Double> scoreMembers) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zadd(key, scoreMembers);
		} else {
			return ((PipelineClusterExt) redisPipeline).zadd(key, scoreMembers);
		}
	}

	@Override
	public Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zadd(key, scoreMembers, params);
		} else {
			return ((PipelineClusterExt) redisPipeline).zadd(key, scoreMembers, params);
		}
	}

	@Override
	public Response<Long> zcard(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zcard(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).zcard(key);
		}
	}

	@Override
	public Response<Long> zcount(String key, double min, double max) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zcount(key, min, max);
		} else {
			return ((PipelineClusterExt) redisPipeline).zcount(key, min, max);
		}
	}

	@Override
	public Response<Double> zincrby(String key, double score, String member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zincrby(key, score, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).zincrby(key, score, member);
		}
	}

	@Override
	public Response<Double> zincrby(String key, double score, String member, ZIncrByParams params) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zincrby(key, score, member, params);
		} else {
			return ((PipelineClusterExt) redisPipeline).zincrby(key, score, member, params);
		}
	}

	@Override
	public Response<Set<String>> zrange(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrange(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrange(key, start, end);
		}
	}

	@Override
	public Response<Set<String>> zrangeByScore(String key, double min, double max) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrangeByScore(key, min, max);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrangeByScore(key, min, max);
		}
	}

	@Override
	public Response<Set<String>> zrangeByScore(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrangeByScore(key, min, max);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrangeByScore(key, min, max);
		}
	}

	@Override
	public Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrangeByScore(key, min, max, offset, count);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrangeByScore(key, min, max, offset, count);
		}
	}

	@Override
	public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrangeByScoreWithScores(key, min, max);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrangeByScoreWithScores(key, min, max);
		}
	}

	@Override
	public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrangeByScoreWithScores(key, min, max, offset, count);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrangeByScoreWithScores(key, min, max, offset, count);
		}
	}

	@Override
	public Response<Set<String>> zrevrangeByScore(String key, double max, double min) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrangeByScore(key, max, min);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrangeByScore(key, max, min);
		}
	}

	@Override
	public Response<Set<String>> zrevrangeByScore(String key, String max, String min) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrangeByScore(key, max, min);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrangeByScore(key, max, min);
		}
	}

	@Override
	public Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrangeByScore(key, max, min, offset, count);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrangeByScore(key, max, min, offset, count);
		}
	}

	@Override
	public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrangeByScoreWithScores(key, max, min);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrangeByScoreWithScores(key, max, min);
		}
	}

	@Override
	public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrangeByScoreWithScores(key, max, min, offset, count);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrangeByScoreWithScores(key, max, min, offset, count);
		}
	}

	@Override
	public Response<Set<Tuple>> zrangeWithScores(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrangeWithScores(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrangeWithScores(key, start, end);
		}
	}

	@Override
	public Response<Long> zrank(String key, String member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrank(key, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrank(key, member);
		}
	}

	@Override
	public Response<Long> zrem(String key, String... member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrem(key, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrem(key, member);
		}
	}

	@Override
	public Response<Long> zremrangeByRank(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zremrangeByRank(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).zremrangeByRank(key, start, end);
		}
	}

	@Override
	public Response<Long> zremrangeByScore(String key, double start, double end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zremrangeByScore(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).zremrangeByScore(key, start, end);
		}
	}

	@Override
	public Response<Set<String>> zrevrange(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrange(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrange(key, start, end);
		}
	}

	@Override
	public Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrangeWithScores(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrangeWithScores(key, start, end);
		}
	}

	@Override
	public Response<Long> zrevrank(String key, String member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrank(key, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrank(key, member);
		}
	}

	@Override
	public Response<Double> zscore(String key, String member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zscore(key, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).zscore(key, member);
		}
	}

	@Override
	public Response<Long> zlexcount(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zlexcount(key, min, max);
		} else {
			return ((PipelineClusterExt) redisPipeline).zlexcount(key, min, max);
		}
	}

	@Override
	public Response<Set<String>> zrangeByLex(String key, String min, String max) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrangeByLex(key, min, max);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrangeByLex(key, min, max);
		}
	}

	@Override
	public Response<Set<String>> zrangeByLex(String key, String min, String max, int offset, int count) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrangeByLex(key, min, max, offset, count);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrangeByLex(key, min, max, offset, count);
		}
	}

	@Override
	public Response<Set<String>> zrevrangeByLex(String key, String max, String min) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrangeByLex(key, max, min);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrangeByLex(key, max, min);
		}
	}

	@Override
	public Response<Set<String>> zrevrangeByLex(String key, String max, String min, int offset, int count) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zrevrangeByLex(key, max, min, offset, count);
		} else {
			return ((PipelineClusterExt) redisPipeline).zrevrangeByLex(key, max, min, offset, count);
		}
	}

	@Override
	public Response<Long> zremrangeByLex(String key, String start, String end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).zremrangeByLex(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).zremrangeByLex(key, start, end);
		}
	}

	@Override
	public Response<Long> bitcount(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).bitcount(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).bitcount(key);
		}
	}

	@Override
	public Response<Long> bitcount(String key, long start, long end) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).bitcount(key, start, end);
		} else {
			return ((PipelineClusterExt) redisPipeline).bitcount(key, start, end);
		}
	}

	@Override
	public Response<Long> pfadd(String key, String... elements) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).pfadd(key, elements);
		} else {
			return ((PipelineClusterExt) redisPipeline).pfadd(key, elements);
		}
	}

	@Override
	public Response<Long> pfcount(String key) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).pfcount(key);
		} else {
			return ((PipelineClusterExt) redisPipeline).pfcount(key);
		}
	}

	@Override
	public Response<Long> geoadd(String key, double longitude, double latitude, String member) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).geoadd(key, longitude, latitude, member);
		} else {
			return ((PipelineClusterExt) redisPipeline).geoadd(key, longitude, latitude, member);
		}
	}

	@Override
	public Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).geoadd(key, memberCoordinateMap);
		} else {
			return ((PipelineClusterExt) redisPipeline).geoadd(key, memberCoordinateMap);
		}
	}

	@Override
	public Response<Double> geodist(String key, String member1, String member2) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).geodist(key, member1, member2);
		} else {
			return ((PipelineClusterExt) redisPipeline).geodist(key, member1, member2);
		}
	}

	@Override
	public Response<Double> geodist(String key, String member1, String member2, GeoUnit unit) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).geodist(key, member1, member2, unit);
		} else {
			return ((PipelineClusterExt) redisPipeline).geodist(key, member1, member2, unit);
		}
	}

	@Override
	public Response<List<String>> geohash(String key, String... members) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).geohash(key, members);
		} else {
			return ((PipelineClusterExt) redisPipeline).geohash(key, members);
		}
	}

	@Override
	public Response<List<GeoCoordinate>> geopos(String key, String... members) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).geopos(key, members);
		} else {
			return ((PipelineClusterExt) redisPipeline).geopos(key, members);
		}
	}

	@Override
	public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius,
			GeoUnit unit) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).georadius(key, longitude, latitude, radius, unit);
		} else {
			return ((PipelineClusterExt) redisPipeline).georadius(key, longitude, latitude, radius, unit);
		}
	}

	@Override
	public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius,
			GeoUnit unit, GeoRadiusParam param) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).georadius(key, longitude, latitude, radius, unit, param);
		} else {
			return ((PipelineClusterExt) redisPipeline).georadius(key, longitude, latitude, radius, unit, param);
		}
	}

	@Override
	public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).georadiusByMember(key, member, radius, unit);
		} else {
			return ((PipelineClusterExt) redisPipeline).georadiusByMember(key, member, radius, unit);
		}
	}

	@Override
	public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit,
			GeoRadiusParam param) {
		if(isSingleMode()) {
			return ((Pipeline) redisPipeline).georadiusByMember(key, member, radius, unit, param);
		} else {
			return ((PipelineClusterExt) redisPipeline).georadiusByMember(key, member, radius, unit, param);
		}
	}


}
