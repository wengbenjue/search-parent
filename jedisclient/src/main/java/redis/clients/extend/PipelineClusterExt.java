package redis.clients.extend;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.commands.RedisPipeline;
import redis.clients.jedis.exceptions.JedisClusterMaxRedirectionsException;
import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;
import redis.clients.util.JedisClusterCRC16;
import redis.clients.util.SafeEncoder;

/**
 * Jedis集群的管道功能实现
 * 
 * @author jimmy.zhou
 *
 */
public class PipelineClusterExt implements RedisPipeline {
	
	// 管道队列 - 命令
	private List<String> pipelineCmds = new ArrayList<String>();
	// 管道队列 - 参数类型
	private List<Class[]> pipelineArgsClz = new ArrayList<Class[]>();
	// 管道队列 - 参数
	private List<List<Object>> pipelineArgs = new ArrayList<List<Object>>();
	// 管道队列 - 返回结果
	private List<Response> pipelineResps = new ArrayList<Response>();
	
	private Map<String, Pipeline> pipelineCache = new HashMap<String, Pipeline>();
	private Map<JedisPool, Jedis> poolConnCache = new HashMap<JedisPool, Jedis>();

	protected JedisClusterConnectionHandler connectionHandler = null;

	public PipelineClusterExt(JedisClusterConnectionHandler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}
	
	
	/**
	 * 各个服务器分别pipeline.sync
	 * @throws InterruptedException 
	 */
	public void sync() {
		runWithRetries(10);
	}
	
	private void runWithRetries(int redirections) {
		if (redirections <= 0) {
			throw new JedisClusterMaxRedirectionsException("Too many Cluster redirections?");
		}
		
		ExecutorService pool = null;
		try {
			// 创建管道，并分配cmd
			int cmdSize = pipelineCmds.size();
			List<Response> data = new ArrayList<Response>();
			for(int i=0; i<cmdSize; i++) {
				String cmd = pipelineCmds.get(i);
				Class[] parameterTypes = pipelineArgsClz.get(i);
				List<Object> args = pipelineArgs.get(i);
				data.add(assignCmd(cmd, parameterTypes, args));
			}
			
			// 多线程执行cmd
			pool = Executors.newFixedThreadPool(4);
			List<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
			for (Pipeline pipeline : pipelineCache.values()) {
				Callable<Boolean> callable = new PipelineSyncCallable(pipeline);
				Future<Boolean> future = pool.submit(callable);
				futures.add(future);
			}
			for (Future<Boolean> future : futures) {
				future.get();
			}
			
			// 同步data
			for(int i=0; i<cmdSize; i++) {
				pipelineResps.get(i).set(data.get(i).data());
			}
			
			// 清空cmds等信息
			pipelineCmds.clear();
			pipelineArgsClz.clear();
			pipelineArgs.clear();
			pipelineResps.clear();
			
		} catch(Exception e) {
			// 打印日志
			e.printStackTrace();
			
			// 等待10秒钟
			for(int i=10; i>0; i--) {
				try {
					System.out.println("jedis cluster pipeline Exception. retries after " + i + " seconds...");
					Thread.sleep(1000l);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			
			// 关闭sync多线程
			if(pool != null) {
				pool.shutdown();
			}
			
			// 释放连接池连接
			for(Jedis jedis : poolConnCache.values()) {
				jedis.close();
			}

			// 释放cache
			pipelineCache.clear();
			poolConnCache.clear();
			
			// 重置slots
			this.connectionHandler.renewSlotCache();
			
			// 重试
			runWithRetries(redirections -1);
		} finally {
			// 关闭sync多线程
			if(pool != null) {
				pool.shutdown();
			}
			
			// 释放连接池连接
			for(Jedis jedis : poolConnCache.values()) {
				jedis.close();
			}

			// 释放cache
			pipelineCache.clear();
			poolConnCache.clear();
			
		}
	}
	
	/**
	 * 执行cmd，分配到不同的pipeline
	 * @param cmd
	 * @param args
	 * @return
	 */
	private Response assignCmd(String cmd, Class[] parameterTypes, List<Object> args) {
		try {
			Method method = this.getClass().getMethod("_" + cmd, parameterTypes);
			return (Response) method.invoke(this, args.toArray());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获得一个key的pipleline
	 * 
	 * @param key
	 */
	public Pipeline pipelineChoise(byte[] key) {
		// key -> slot -> jedis -> ip+port
		int slot = JedisClusterCRC16.getSlot(key);
		
		// poolConnCache (get and set)
		JedisPool jedisPool = connectionHandler.getCache().getSlotPool(slot);
		Jedis connection = poolConnCache.get(jedisPool);
		if(connection == null) {
			connection = connectionHandler.getConnectionFromSlot(slot);
			poolConnCache.put(jedisPool, connection);
		}
		
		// create node key
		String host = connection.getClient().getHost();
		int port = connection.getClient().getPort();
		String nodeKey = getNodeKey(host, port);

		// pipelineCache (get and set)
		Pipeline pipeline = pipelineCache.get(nodeKey);
		if (pipeline == null) {
			pipeline = connection.pipelined();
			pipelineCache.put(nodeKey, pipeline);
		}
		return pipeline;
	}

	public static String getNodeKey(String host, int port) {
		return host + ":" + port;
	}
	
	// XXX 对外接口
	public Response<List<String>> hmget(String key, String... fields) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("hmget");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, fields}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> hmset(String key, Map<String, String> hash) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("hmset");
		pipelineArgsClz.add(new Class[]{String.class, Map.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, hash}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> hset(String key, String field, String value) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("hset");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, field, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> hsetnx(String key, String field, String value) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("hsetnx");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, field, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<String>> lrange(String key, long start, long end) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("lrange");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> move(String key, int dbIndex) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("move");
		pipelineArgsClz.add(new Class[]{String.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, dbIndex}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> append(String key, String value) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("append");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<String>> blpop(String key) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("blpop");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<String>> brpop(String key) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("brpop");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> decr(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("decr");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> decrBy(String key, long integer) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("decrBy");
		pipelineArgsClz.add(new Class[]{String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, integer}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> del(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("del");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> echo(String key, String string) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("echo");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, string}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Boolean> exists(String key) {
		Response<Boolean> res = new Response<Boolean>(BuilderFactory.BOOLEAN);
		pipelineCmds.add("exists");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> expire(String key, int seconds) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("expire");
		pipelineArgsClz.add(new Class[]{String.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, seconds}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> expireAt(String key, long unixTime) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("expireAt");
		pipelineArgsClz.add(new Class[]{String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, unixTime}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> get(String key) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("get");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Boolean> getbit(String key, long offset) {
		Response<Boolean> res = new Response<Boolean>(BuilderFactory.BOOLEAN);
		pipelineCmds.add("getbit");
		pipelineArgsClz.add(new Class[]{String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, offset}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> getrange(String key, long startOffset, long endOffset) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("getrange");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, startOffset, endOffset}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> getSet(String key, String value) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("getSet");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> hdel(String key, String... field) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("hdel");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, field}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Boolean> hexists(String key, String field) {
		Response<Boolean> res = new Response<Boolean>(BuilderFactory.BOOLEAN);
		pipelineCmds.add("hexists");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, field}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> hget(String key, String field) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("hget");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, field}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Map<String, String>> hgetAll(String key) {
		Response<Map<String, String>> res = new Response<Map<String, String>>(BuilderFactory.STRING_MAP);
		pipelineCmds.add("hgetAll");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> hincrBy(String key, String field, long value) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("hincrBy");
		pipelineArgsClz.add(new Class[]{String.class, String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, field, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> hkeys(String key) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("hkeys");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> hlen(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("hlen");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<String>> hvals(String key) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("hvals");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> incr(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("incr");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> incrBy(String key, long integer) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("incrBy");
		pipelineArgsClz.add(new Class[]{String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, integer}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> lindex(String key, long index) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("lindex");
		pipelineArgsClz.add(new Class[]{String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, index}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> linsert(String key, LIST_POSITION where, String pivot, String value) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("linsert");
		pipelineArgsClz.add(new Class[]{String.class, LIST_POSITION.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, where, pivot, value}));
		pipelineResps.add(res);
		return res;
	}
	
	public Response<Long> llen(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("llen");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> lpop(String key) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("lpop");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> lpush(String key, String... string) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("lpush");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, string}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> lpushx(String key, String... string) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("lpushx");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, string}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> lrem(String key, long count, String value) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("lrem");
		pipelineArgsClz.add(new Class[]{String.class, long.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, count, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> lset(String key, long index, String value) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("lset");
		pipelineArgsClz.add(new Class[]{String.class, long.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, index, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> ltrim(String key, long start, long end) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("ltrim");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> persist(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("persist");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> rpop(String key) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("rpop");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> rpush(String key, String... string) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("rpush");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, string}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> rpushx(String key, String... string) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("rpushx");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, string}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> sadd(String key, String... member) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("sadd");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> scard(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("scard");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> set(String key, String value) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("set");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Boolean> setbit(String key, long offset, boolean value) {
		Response<Boolean> res = new Response<Boolean>(BuilderFactory.BOOLEAN);
		pipelineCmds.add("setbit");
		pipelineArgsClz.add(new Class[]{String.class, long.class, boolean.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, offset, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> setex(String key, int seconds, String value) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("setex");
		pipelineArgsClz.add(new Class[]{String.class, int.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, seconds, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> setnx(String key, String value) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("setnx");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> setrange(String key, long offset, String value) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("setrange");
		pipelineArgsClz.add(new Class[]{String.class, long.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, offset, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Boolean> sismember(String key, String member) {
		Response<Boolean> res = new Response<Boolean>(BuilderFactory.BOOLEAN);
		pipelineCmds.add("sismember");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> smembers(String key) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("smembers");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<String>> sort(String key) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("sort");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<String>> sort(String key, SortingParams sortingParameters) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("sort");
		pipelineArgsClz.add(new Class[]{String.class, SortingParams.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, sortingParameters}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> spop(String key) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("spop");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> spop(String key, long count) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("spop");
		pipelineArgsClz.add(new Class[]{String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> srandmember(String key) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("srandmember");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<String>> srandmember(String key, int count) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("srandmember");
		pipelineArgsClz.add(new Class[]{String.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> srem(String key, String... member) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("srem");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> strlen(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("strlen");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> substr(String key, int start, int end) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("substr");
		pipelineArgsClz.add(new Class[]{String.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> ttl(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("ttl");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> type(String key) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("type");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zadd(String key, double score, String member) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zadd");
		pipelineArgsClz.add(new Class[]{String.class, double.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, score, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zadd(String key, Map<String, Double> scoreMembers) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zadd");
		pipelineArgsClz.add(new Class[]{String.class, Map.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, scoreMembers}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zcount(String key, double min, double max) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zcount");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zcount(String key, String min, String max) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zcount");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Double> zincrby(String key, double score, String member) {
		Response<Double> res = new Response<Double>(BuilderFactory.DOUBLE);
		pipelineCmds.add("zincrby");
		pipelineArgsClz.add(new Class[]{String.class, double.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, score, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrangeByScore(String key, double min, double max) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrangeByScore(String key, String min, String max) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrangeByScoreWithScores");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrangeByScoreWithScores");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrangeByScoreWithScores");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrangeByScoreWithScores");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrevrangeByScore(String key, double max, double min) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrevrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrevrangeByScore(String key, String max, String min) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrevrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrevrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrevrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrevrangeByScoreWithScores");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrevrangeByScoreWithScores");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrevrangeByScoreWithScores");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrevrangeByScoreWithScores");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrangeWithScores(String key, long start, long end) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrangeWithScores");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zrank(String key, String member) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zrank");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zrem(String key, String... member) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zrem");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zremrangeByRank(String key, long start, long end) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zremrangeByRank");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zremrangeByScore(String key, double start, double end) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zremrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zremrangeByScore(String key, String start, String end) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zremrangeByScore");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrevrange(String key, long start, long end) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrevrange");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long end) {
		Response<Set<Tuple>> res = new Response<Set<Tuple>>(BuilderFactory.TUPLE_ZSET_BINARY);
		pipelineCmds.add("zrevrangeWithScores");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zrevrank(String key, String member) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zrevrank");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Double> zscore(String key, String member) {
		Response<Double> res = new Response<Double>(BuilderFactory.DOUBLE);
		pipelineCmds.add("zscore");
		pipelineArgsClz.add(new Class[]{String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> bitcount(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("bitcount");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> bitcount(String key, long start, long end) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("bitcount");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<byte[]> dump(String key) {
		Response<byte[]> res = new Response<byte[]>(BuilderFactory.BYTE_ARRAY);
		pipelineCmds.add("dump");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> migrate(String host, int port, String key, int destinationDb, int timeout) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("migrate");
		pipelineArgsClz.add(new Class[]{String.class, int.class, String.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{host, port, key, destinationDb, timeout}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> objectRefcount(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("objectRefcount");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> objectEncoding(String key) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("objectEncoding");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> objectIdletime(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("objectIdletime");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> pexpire(String key, int milliseconds) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("pexpire");
		pipelineArgsClz.add(new Class[]{String.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, milliseconds}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> pexpire(String key, long milliseconds) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("pexpire");
		pipelineArgsClz.add(new Class[]{String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, milliseconds}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> pexpireAt(String key, long millisecondsTimestamp) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("pexpireAt");
		pipelineArgsClz.add(new Class[]{String.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, millisecondsTimestamp}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> pttl(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("pttl");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> restore(String key, int ttl, byte[] serializedValue) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("restore");
		pipelineArgsClz.add(new Class[]{String.class, int.class, byte[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, ttl, serializedValue}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Double> incrByFloat(String key, double increment) {
		Response<Double> res = new Response<Double>(BuilderFactory.DOUBLE);
		pipelineCmds.add("incrByFloat");
		pipelineArgsClz.add(new Class[]{String.class, double.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, increment}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> psetex(String key, int milliseconds, String value) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("psetex");
		pipelineArgsClz.add(new Class[]{String.class, int.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, milliseconds, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> psetex(String key, long milliseconds, String value) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("psetex");
		pipelineArgsClz.add(new Class[]{String.class, long.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, milliseconds, value}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> set(String key, String value, String nxxx) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("set");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, value, nxxx}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> set(String key, String value, String nxxx, String expx, int time) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("set");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class, String.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, value, nxxx, expx, time}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Double> hincrByFloat(String key, String field, double increment) {
		Response<Double> res = new Response<Double>(BuilderFactory.DOUBLE);
		pipelineCmds.add("hincrByFloat");
		pipelineArgsClz.add(new Class[]{String.class, String.class, double.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, field, increment}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> eval(String key,String script) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("eval");
		pipelineArgsClz.add(new Class[]{String.class,String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key,script}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> eval(String key,String script, int numKeys, String... args) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("eval");
		pipelineArgsClz.add(new Class[]{String.class,String.class, int.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key,script, numKeys, args}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> evalsha(String key,String sha1, int numKeys, String... args) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("evalsha");
		pipelineArgsClz.add(new Class[]{String.class,String.class, int.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key,sha1, numKeys, args}));
		pipelineResps.add(res);
		return res;
	}

	public Response<String> echo(String string) {
		Response<String> res = new Response<String>(BuilderFactory.STRING);
		pipelineCmds.add("echo");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{string}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zadd(String key, double score, String member, ZAddParams params) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zadd");
		pipelineArgsClz.add(new Class[]{String.class, double.class, String.class, ZAddParams.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, score, member, params}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zadd");
		pipelineArgsClz.add(new Class[]{String.class, Map.class, ZAddParams.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, scoreMembers, params}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zcard(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zcard");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Double> zincrby(String key, double score, String member, ZIncrByParams params) {
		Response<Double> res = new Response<Double>(BuilderFactory.DOUBLE);
		pipelineCmds.add("zincrby");
		pipelineArgsClz.add(new Class[]{String.class, double.class, String.class, ZIncrByParams.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, score, member, params}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrange(String key, long start, long end) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrange");
		pipelineArgsClz.add(new Class[]{String.class, long.class, long.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zlexcount(String key, String min, String max) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zlexcount");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrangeByLex(String key, String min, String max) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrangeByLex");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrangeByLex(String key, String min, String max, int offset, int count) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrangeByLex");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, min, max, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrevrangeByLex(String key, String max, String min) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrevrangeByLex");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Set<String>> zrevrangeByLex(String key, String max, String min, int offset, int count) {
		Response<Set<String>> res = new Response<Set<String>>(BuilderFactory.STRING_SET);
		pipelineCmds.add("zrevrangeByLex");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class, int.class, int.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, max, min, offset, count}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> zremrangeByLex(String key, String start, String end) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("zremrangeByLex");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, start, end}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> pfadd(String key, String... elements) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("pfadd");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, elements}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> pfcount(String key) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("pfcount");
		pipelineArgsClz.add(new Class[]{String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> geoadd(String key, double longitude, double latitude, String member) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("geoadd");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, longitude, latitude, member}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
		Response<Long> res = new Response<Long>(BuilderFactory.LONG);
		pipelineCmds.add("geoadd");
		pipelineArgsClz.add(new Class[]{String.class, Map.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, memberCoordinateMap}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Double> geodist(String key, String member1, String member2) {
		Response<Double> res = new Response<Double>(BuilderFactory.DOUBLE);
		pipelineCmds.add("geodist");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member1, member2}));
		pipelineResps.add(res);
		return res;
	}

	public Response<Double> geodist(String key, String member1, String member2, GeoUnit unit) {
		Response<Double> res = new Response<Double>(BuilderFactory.DOUBLE);
		pipelineCmds.add("geodist");
		pipelineArgsClz.add(new Class[]{String.class, String.class, String.class, GeoUnit.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member1, member2, unit}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<String>> geohash(String key, String... members) {
		Response<List<String>> res = new Response<List<String>>(BuilderFactory.STRING_LIST);
		pipelineCmds.add("geohash");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, members}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<GeoCoordinate>> geopos(String key, String... members) {
		Response<List<GeoCoordinate>> res = new Response<List<GeoCoordinate>>(BuilderFactory.GEO_COORDINATE_LIST);
		pipelineCmds.add("geopos");
		pipelineArgsClz.add(new Class[]{String.class, String[].class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, members}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
		Response<List<GeoRadiusResponse>> res = new Response<List<GeoRadiusResponse>>(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
		pipelineCmds.add("georadius");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class, double.class, GeoUnit.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, longitude, latitude, radius, unit}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
		Response<List<GeoRadiusResponse>> res = new Response<List<GeoRadiusResponse>>(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
		pipelineCmds.add("georadius");
		pipelineArgsClz.add(new Class[]{String.class, double.class, double.class, double.class, GeoUnit.class,});
		pipelineArgs.add(Arrays.asList(new Object[]{key, longitude, latitude, radius, unit, param}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit) {
		Response<List<GeoRadiusResponse>> res = new Response<List<GeoRadiusResponse>>(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
		pipelineCmds.add("georadiusByMember");
		pipelineArgsClz.add(new Class[]{String.class, String.class, double.class, GeoUnit.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member, radius, unit}));
		pipelineResps.add(res);
		return res;
	}

	public Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius, GeoUnit unit,	GeoRadiusParam param) {
		Response<List<GeoRadiusResponse>> res = new Response<List<GeoRadiusResponse>>(BuilderFactory.GEORADIUS_WITH_PARAMS_RESULT);
		pipelineCmds.add("georadiusByMember");
		pipelineArgsClz.add(new Class[]{String.class, String.class, double.class, GeoUnit.class,	GeoRadiusParam.class});
		pipelineArgs.add(Arrays.asList(new Object[]{key, member, radius, unit, param}));
		pipelineResps.add(res);
		return res;
	}
	
	// XXX 管道分配和调用
	public Response<List<String>> _hmget(String key, String... fields) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hmget(key, fields);
	}

	public Response<String> _hmset(String key, Map<String, String> hash) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hmset(key, hash);
	}
	
	public Response<Long> _hset(String key, String field, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hset(key, field, value);
	}
	
	public Response<Long> _hsetnx(String key, String field, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hsetnx(key, field, value);
	}
	
	public Response<List<String>> _lrange(String key, long start, long end) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.lrange(key, start, end);
	}

	public Response<Long> _move(String key, int dbIndex) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.move(key, dbIndex);
	}
	
	public Response<Long> _append(String key, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.append(key, value);
	}
	
	public Response<List<String>> _blpop(String key) {
		String[] temp = new String[1];
		temp[0] = key;
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.blpop(temp);
    }
	
	public Response<List<String>> _brpop(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	    String[] temp = new String[1];
	    temp[0] = key;
	    return pipeline.brpop(temp);
	}
	
	public Response<Long> _decr(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.decr(key);
    }
	
	public Response<Long> _decrBy(String key, long integer) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.decrBy(key, integer);
	}
	
	public Response<Long> _del(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.del(key);
    }

	public Response<String> _echo(String key, String string) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.echo(string);
	}
	
	public Response<Boolean> _exists(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.exists(key);
    }

	public Response<Long> _expire(String key, int seconds) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.expire(key, seconds);
    }
	
	public Response<Long> _expireAt(String key, long unixTime) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.expireAt(key, unixTime);
	}

	public Response<String> _get(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.get(key);
    }
	
	public Response<Boolean> _getbit(String key, long offset) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.getbit(key, offset);
    }

	public Response<String> _getrange(String key, long startOffset, long endOffset) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.getrange(key, startOffset, endOffset);
	}
	
	public Response<String> _getSet(String key, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return  pipeline.getSet(key, value);
	}

	public Response<Long> _hdel(String key, String... field) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hdel(key, field);
    }

	public Response<Boolean> _hexists(String key, String field) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hexists(key, field);
	}
	
	public Response<String> _hget(String key, String field) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hget(key, field);
	}

	public Response<Map<String, String>> _hgetAll(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hgetAll(key);
	}
    
	public Response<Long> _hincrBy(String key, String field, long value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	    return	pipeline.hincrBy(key, field, value);
	}

	public Response<Set<String>> _hkeys(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hkeys(key);
	}

	public Response<Long> _hlen(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hlen(key);
	}

	public Response<List<String>> _hvals(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.hvals(key);
	}

	public Response<Long> _incr(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.incr(key);
	}

	public Response<Long> _incrBy(String key, long integer) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.incrBy(key, integer);
	}

	public Response<String> _lindex(String key, long index) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.lindex(key, index);
    }

	public Response<Long> _linsert(String key, LIST_POSITION where, String pivot, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.linsert(key, where, pivot, value);
	}

	public Response<Long> _llen(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.llen(key);
	}

	public Response<String> _lpop(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.lpop(key);
    }

	public Response<Long> _lpush(String key, String... string) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.lpush(key, string);
	}

	public Response<Long> _lpushx(String key, String... string) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.lpushx(key, string);
	}

	public Response<Long> _lrem(String key, long count, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.lrem(key, count, value);
    }
	
	public Response<String> _lset(String key, long index, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.lset(key, index, value);
	}

	public Response<String> _ltrim(String key, long start, long end) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.ltrim(key, start, end);
	}

	public Response<Long> _persist(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.persist(key);
    }

	public Response<String> _rpop(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.rpop(key);
	}

	public Response<Long> _rpush(String key, String... string) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.rpush(key, string);
    }

	public Response<Long> _rpushx(String key, String... string) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.rpushx(key, string);
	}

	public Response<Long> _sadd(String key, String... member) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.sadd(key, member);
	}
	
	public Response<Long> _scard(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.scard(key);
    }

	public Response<String> _set(String key, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.set(key, value);
    }

	public Response<Boolean> _setbit(String key, long offset, boolean value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.setbit(key, offset, value);
	}

	public Response<String> _setex(String key, int seconds, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.setex(key, seconds, value);
	}
	
	public Response<Long> _setnx(String key, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.setnx(key, value);
	}
	
	public Response<Long> _setrange(String key, long offset, String value) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.setrange(key, offset, value);
	}

	public Response<Boolean> _sismember(String key, String member) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.sismember(key, member);
    }

	public Response<Set<String>> _smembers(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.smembers(key);
	}

	public Response<List<String>> _sort(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.sort(key);
	}

	public Response<List<String>> _sort(String key, SortingParams sortingParameters) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.sort(key, sortingParameters);
	}

	public Response<String> _spop(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.spop(key);
    }

	public Response<Set<String>> _spop(String key, long count) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.spop(key, count);
	}

    public Response<String> _srandmember(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.srandmember(key);
	}

    public Response<List<String>> _srandmember(String key, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.srandmember(key, count);
    }

    public Response<Long> _srem(String key, String... member) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.srem(key, member);
    }

    public Response<Long> _strlen(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.strlen(key);
    }

    public Response<String> _substr(String key, int start, int end) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.substr(key, start, end);
    }

    public Response<Long> _ttl(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.ttl(key);
    }
	
    public Response<String> _type(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.type(key);
    }

    public Response<Long> _zadd(String key, double score, String member) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zadd(key, score, member);
    }

  
    public Response<Long> _zadd(String key, Map<String, Double> scoreMembers) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zadd(key, scoreMembers);
    }

    public Response<Long> _zcount(String key, double min, double max) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zcount(key, min, max);
    }

    public Response<Long> _zcount(String key, String min, String max) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zcount(key, min, max);
    }
    
    public Response<Double> _zincrby(String key, double score, String member) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zincrby(key, score, member);
    }

    public Response<Set<String>> _zrangeByScore(String key, double min, double max) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrangeByScore(key, min, max);
    }
    
    public Response<Set<String>> _zrangeByScore(String key, String min, String max) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
        return pipeline.zrangeByScore(key, min, max);
    }

    public Response<Set<String>> _zrangeByScore(String key, double min, double max, int offset, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrangeByScore(key, min, max, offset, count);
    }

    public Response<Set<String>> _zrangeByScore(String key, String min, String max, int offset, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrangeByScore(key, min, max, offset, count);
    }
    
    public Response<Set<Tuple>> _zrangeByScoreWithScores(String key, double min, double max) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrangeByScoreWithScores(key, min, max);
    }

    public Response<Set<Tuple>> _zrangeByScoreWithScores(String key, String min, String max) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrangeByScoreWithScores(key, min, max);
    }
    
    public Response<Set<Tuple>> _zrangeByScoreWithScores(String key, double min, double max, int offset, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }
    
    public Response<Set<Tuple>> _zrangeByScoreWithScores(String key, String min, String max, int offset, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrangeByScoreWithScores(key, min, max, offset, count);
    }
    
    public Response<Set<String>> _zrevrangeByScore(String key, double max, double min) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeByScore(key, max, min);
    }
    
    public Response<Set<String>> _zrevrangeByScore(String key, String max, String min) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeByScore(key, max, min);
    }
    
    public Response<Set<String>> _zrevrangeByScore(String key, double max, double min, int offset, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }
    
    public Response<Set<String>> _zrevrangeByScore(String key, String max, String min, int offset, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeByScore(key, max, min, offset, count);
    }

    public Response<Set<Tuple>> _zrevrangeByScoreWithScores(String key, double max, double min) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }
    
    public Response<Set<Tuple>> _zrevrangeByScoreWithScores(String key, String max, String min) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeByScoreWithScores(key, max, min);
    }
    
    
    public Response<Set<Tuple>> _zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    public Response<Set<Tuple>> _zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }
    
    public Response<Set<Tuple>> _zrangeWithScores(String key, long start, long end) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrangeWithScores(key, start, end);
    }
    
    public Response<Long> _zrank(String key, String member) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrank(key, member);
    }

    public Response<Long> _zrem(String key, String... member) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrem(key, member);
    }
    
    public Response<Long> _zremrangeByRank(String key, long start, long end) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zremrangeByRank(key, start, end);
    }
    
    public Response<Long> _zremrangeByScore(String key, double start, double end) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zremrangeByScore(key, start, end);
    }
    
    public Response<Long> _zremrangeByScore(String key, String start, String end) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zremrangeByScore(key, start, end);
    }

    public Response<Set<String>> _zrevrange(String key, long start, long end) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrange(key, start, end);
    }
    
    public Response<Set<Tuple>> _zrevrangeWithScores(String key, long start, long end) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrangeWithScores(key, start, end);
    }
    
    public Response<Long> _zrevrank(String key, String member) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zrevrank(key, member);
    }
    
    public Response<Double> _zscore(String key, String member) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.zscore(key, member);
    }
    
    public Response<Long> _bitcount(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.bitcount(key);
    }
    
    public Response<Long> _bitcount(String key, long start, long end) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.bitcount(key, start, end);
    }
    
    public Response<byte[]> _dump(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.dump(key);
    }
    
    public Response<String> _migrate(String host, int port, String key, int destinationDb, int timeout) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.migrate(host, port, key, destinationDb, timeout);
    }
    
    public Response<Long> _objectRefcount(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.objectRefcount(key);
    }
    
    public Response<String> _objectEncoding(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.objectEncoding(key);
    }
    
    public Response<Long> _objectIdletime(String key) {
    	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
    	return pipeline.objectIdletime(key);
    }
    
    @Deprecated
    public Response<Long> _pexpire(String key, int milliseconds) {
      return pexpire(key, (long) milliseconds);
    }
 
   public Response<Long> _pexpire(String key, long milliseconds) {
      	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
      	return pipeline.pexpire(key, milliseconds);
   }
    
   public Response<Long> _pexpireAt(String key, long millisecondsTimestamp) {
	   	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   	return pipeline.pexpireAt(key, millisecondsTimestamp);
   }
    
   public Response<Long> _pttl(String key) {
	   	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   	return pipeline.pttl(key);
   }
   
   public Response<String> _restore(String key, int ttl, byte[] serializedValue) {
	   	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   	return pipeline.restore(key, ttl, serializedValue);
   }
   
   public Response<Double> _incrByFloat(String key, double increment) {
	   	Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   	return pipeline.incrByFloat(key, increment);
   }
   
   @Deprecated
   public Response<String> _psetex(String key, int milliseconds, String value) {
     return psetex(key, (long) milliseconds, value);
   }

   public Response<String> _psetex(String key, long milliseconds, String value) {
	   Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   return pipeline.psetex(key, milliseconds, value);
   }
   
   public Response<String> _set(String key, String value, String nxxx) {
	   Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   return pipeline.set(key, value, nxxx);
   }
   
   public Response<String> _set(String key, String value, String nxxx, String expx, int time) {
	   Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   return pipeline.set(key, value, nxxx, expx, time);
   }
   
   public Response<Double> _hincrByFloat(String key, String field, double increment) {
	   Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   return pipeline.hincrByFloat(key, field, increment);
   }
   
   public Response<String> _eval(String key,String script) {
	    return this.eval(key,script, 0);
   }

   public Response<String> _eval(String key,String script, int numKeys, String... args) {
	   Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   return pipeline.eval(script, numKeys, args);
   }
   
   public Response<String> _evalsha(String key,String sha1, int numKeys, String... args) {
	   Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	   return  pipeline.evalsha(sha1, numKeys, args);
   }
   
	public Response<String> _echo(String string) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(string));
		 return pipeline.echo(string);
	}

	public Response<Long> _zadd(String key, double score, String member, ZAddParams params) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.zadd(key, score, member, params);
	}

	public Response<Long> _zadd(String key, Map<String, Double> scoreMembers, ZAddParams params) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.zadd(key, scoreMembers, params);
	}

	public Response<Long> _zcard(String key) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
	     return pipeline.zcard(key);
	}

	public Response<Double> _zincrby(String key, double score, String member, ZIncrByParams params) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.zincrby(key, score, member, params);
	}

	public Response<Set<String>> _zrange(String key, long start, long end) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.zrange(key, start, end);
	}

	public Response<Long> _zlexcount(String key, String min, String max) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.zlexcount(key, min, max);
	}

	public Response<Set<String>> _zrangeByLex(String key, String min, String max) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.zrangeByLex(key, min, max);
	}

	public Response<Set<String>> _zrangeByLex(String key, String min, String max, int offset, int count) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.zrangeByLex(key, min, max, offset, count);
	}

	public Response<Set<String>> _zrevrangeByLex(String key, String max, String min) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.zrevrangeByLex(key, max, min);
	}

	public Response<Set<String>> _zrevrangeByLex(String key, String max, String min, int offset, int count) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.zrevrangeByLex(key, max, min, offset, count);
	}

	public Response<Long> _zremrangeByLex(String key, String start, String end) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.zremrangeByLex(key, start, end);
	}

	public Response<Long> _pfadd(String key, String... elements) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.pfadd(key, elements);
	}

	public Response<Long> _pfcount(String key) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.pfcount(key);
	}

	public Response<Long> _geoadd(String key, double longitude, double latitude, String member) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.geoadd(key, longitude, latitude, member);
	}

	public Response<Long> _geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.geoadd(key, memberCoordinateMap);
	}

	public Response<Double> _geodist(String key, String member1, String member2) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.geodist(key, member1, member2);
	}

	public Response<Double> _geodist(String key, String member1, String member2, GeoUnit unit) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.geodist(key, member1, member2);
	}

	public Response<List<String>> _geohash(String key, String... members) {
		Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		return pipeline.geohash(key, members);
	}

	public Response<List<GeoCoordinate>> _geopos(String key, String... members) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.geopos(key, members);
	}

	public Response<List<GeoRadiusResponse>> _georadius(String key, double longitude, double latitude, double radius, GeoUnit unit) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.georadius(key, longitude, latitude, radius, unit);
	}

	public Response<List<GeoRadiusResponse>> _georadius(String key, double longitude, double latitude, double radius, GeoUnit unit, GeoRadiusParam param) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.georadius(key, longitude, latitude, radius, unit, param);
	}

	public Response<List<GeoRadiusResponse>> _georadiusByMember(String key, String member, double radius, GeoUnit unit) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.georadiusByMember(key, member, radius, unit);
	}

	public Response<List<GeoRadiusResponse>> _georadiusByMember(String key, String member, double radius, GeoUnit unit,	GeoRadiusParam param) {
		 Pipeline pipeline = pipelineChoise(SafeEncoder.encode(key));
		 return pipeline.georadiusByMember(key, member, radius, unit, param);
	}
	
}

