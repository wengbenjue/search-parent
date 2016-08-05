package com.aug3.storage.mongoclient.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A PropertyLoader is associated with a property file defined as a class
 * resources, and loads the data from the resource when initiated.
 * 
 * @author Roger.xia
 * @since Sep 11th 2012
 * 
 */
@SuppressWarnings("serial")
public class PropertyLoader extends Properties {

	private String resourceName;

	public PropertyLoader(String configResource) {
		resourceName = configResource;
		load();
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		String val = getProperty(key);
		if (val == null) {
			val = defaultValue;
		}
		return val;
	}

	@Override
	public String getProperty(String key) {

		return super.getProperty(key);
	}

	/**
	 * Reads a property list (key and element pairs) from the input byte stream.
	 * The input stream is in a simple line-oriented format as specified in
	 * {@link #load(java.io.Reader) load(Reader)} and is assumed to use the ISO
	 * 8859-1 character encoding; that is each byte is one Latin1 character.
	 * 
	 * If you would like to load more than one properties file, make sure keys
	 * specified in different files differs by using name space.
	 * 
	 * @throws RuntimeException
	 *             if it is unable to load the data.
	 */
	private void load() {

		try {

			InputStream in = PropertyLoader.class
					.getResourceAsStream(resourceName);

			if (in == null) {
				return;
			}

			try {
				this.load(in);
			} finally {
				in.close();
			}

		} catch (IOException e) {

			throw new RuntimeException(
					"failed loading configuration data from " + resourceName, e);

		}
	}

}
