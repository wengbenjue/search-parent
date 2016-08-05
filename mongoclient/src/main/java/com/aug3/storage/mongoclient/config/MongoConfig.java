package com.aug3.storage.mongoclient.config;

import java.util.Properties;

public class MongoConfig {

	private final String MONGO_CONFIG_FILE = "/common-mongodb.properties";

	private Properties props = new PropertyLoader(MONGO_CONFIG_FILE);

	public String getProperty(String key) {
		return props.getProperty(key);
	}

	public String getProperty(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}

	public int getIntProperty(String key) {
		String value = props.getProperty(key);
		return Integer.parseInt(value);
	}

	public int getIntProperty(String key, int defaultValue) {
		String value = props.getProperty(key);
		if (value == null || value.length() == 0) {
			return defaultValue;
		} else {
			return Integer.parseInt(value);
		}
	}

	public boolean getBooleanProperty(String key, boolean defaultValue) {
		String value = props.getProperty(key);
		if (value == null || value.length() == 0) {
			return defaultValue;
		} else {
			return Boolean.valueOf(value);
		}
	}

}
