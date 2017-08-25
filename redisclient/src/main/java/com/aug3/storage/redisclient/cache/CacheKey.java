package com.aug3.storage.redisclient.cache;

public class CacheKey {

	private String id;
	private String key;
	private int ttl;// unit -> d,h,m,s

	public String getKey(String placeholder) {
		if (placeholder == null)
			return key;
		return key.replace("{#placeholder}", placeholder).trim();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public int getTtl() {
		return ttl;
	}

	public void setTtl(int ttl) {
		this.ttl = ttl;
	}

}
