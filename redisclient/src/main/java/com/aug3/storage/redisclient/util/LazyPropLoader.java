package com.aug3.storage.redisclient.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A LazyPropLoader is associated with a property file defined as a class
 * resources, and only loads the data from the resource when method
 * <code>getProperty(...)</code> is called.
 * 
 * @author Roger.xia
 * 
 */
@SuppressWarnings("serial")
public class LazyPropLoader extends Properties {

	private boolean isLoaded = false;

	private String resourceName;

	public LazyPropLoader(String configResource) {
		resourceName = configResource;
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
		if (!isLoaded) {
			load();
		}
		return super.getProperty(key);
	}

	/**
	 * Loads the properties from the associated class resource.
	 * 
	 * @throws RuntimeException
	 *             if it is unable to load the data.
	 */
	private void load() {

		try {

			InputStream in = LazyPropLoader.class
					.getResourceAsStream(resourceName);

			if (in == null) {
				return;
			}

			try {
				this.load(in);

				isLoaded = true;

			} finally {
				in.close();
			}

		} catch (IOException e) {

			throw new RuntimeException(
					"failed loading configuration data from " + resourceName, e);

		}
	}

}
