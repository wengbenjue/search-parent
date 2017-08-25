package redis.clients.extend;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import redis.clients.jedis.Jedis;

public class JedisClusterKeysCallable implements Callable<Set<String>> {
	
	Jedis connection;
	String pattern;
	
	
	public JedisClusterKeysCallable(Jedis connection, String pattern){
		this.connection = connection;
		this.pattern = pattern;
	}

	@Override
	public Set<String> call() throws Exception {
		Set<String> keys = null;
		try {
			keys = connection.keys(pattern);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(connection != null) {
				connection.close();
			}
		}
		if(keys == null) {
			keys = new HashSet<String>();
		}
		return keys;
	}
	

}
