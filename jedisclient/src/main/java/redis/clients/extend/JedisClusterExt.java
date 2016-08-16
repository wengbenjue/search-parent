package redis.clients.extend;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

/**
 * JedisCluster扩展类 实现集群下的pipeline功能
 * 
 * @author jimmy.zhou
 *
 */
public class JedisClusterExt extends JedisCluster {

	protected PipelineClusterExt pipeline = null;

	public JedisClusterExt(Set<HostAndPort> nodes, int timeout, final GenericObjectPoolConfig poolConfig) {
		super(nodes, timeout, poolConfig);
	}

	public PipelineClusterExt pipelined() {
		pipeline = new PipelineClusterExt(connectionHandler);
		return pipeline;
	}

	
	/**
	 * 对于大量节点的集群，可能存在性能问题。不建议广泛使用。
	 * 
	 * @param pattern
	 * @return
	 */
	public Set<String> keys(String pattern) {
		Set<String> result = new HashSet<String>();
		Map<String, JedisPool> nodes = connectionHandler.getCache().getNodes();
		
//		// 单线程模式
//		for (JedisPool node : nodes.values()) {
//			Jedis connection = node.getResource();
//			Set<String> keys = connection.keys(pattern);
//			result.addAll(keys);
//			connection.close();
//		}
//		return result;
		
		// 多线程模式
		ExecutorService pool = Executors.newFixedThreadPool(4);
		List<Future<Set<String>>> futures = new ArrayList<Future<Set<String>>>();
		for (JedisPool node : nodes.values()) {
			Jedis connection = node.getResource();
			Callable<Set<String>> callable = new JedisClusterKeysCallable(connection, pattern);
			Future<Set<String>> future = pool.submit(callable);
			futures.add(future);
		}
		for (Future<Set<String>> future : futures) {
			try {
				Set<String> set = future.get();
				if (set != null) {
					result.addAll(set);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		pool.shutdown();
		return result;
	}

}
