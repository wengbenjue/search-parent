package redis.clients.extend;

import java.util.concurrent.Callable;

import redis.clients.jedis.Pipeline;

/**
 * 用于集群内管道命令多线程同步
 * 
 * @author jimmy.zhou
 *
 */
public class PipelineSyncCallable implements Callable<Boolean> {
	
	Pipeline pipeline;
	
	public PipelineSyncCallable(Pipeline pipeline) {
		this.pipeline = pipeline;
	}

	@Override
	public Boolean call() throws Exception {
		pipeline.sync();
		return true;
	}
	
}
