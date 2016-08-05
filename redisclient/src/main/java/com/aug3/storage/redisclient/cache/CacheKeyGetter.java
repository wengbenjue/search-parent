package com.aug3.storage.redisclient.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.aug3.storage.redisclient.RedisPropLoader;
import com.aug3.storage.redisclient.util.Parser;

public class CacheKeyGetter {

	private final static String domain = (new RedisPropLoader()).getProperty("redis.cache.domain", "global");

	private static ConcurrentHashMap<String, CacheKey> keysMap = new ConcurrentHashMap<String, CacheKey>();

	public static CacheKey getKey(String id) {
		if (keysMap.isEmpty()) {
			loadKeys();
		}
		return keysMap.get(id);
	}

	protected static void loadKeys() {

		boolean global = "global".equals(domain);

		InputStream is = CacheKeyGetter.class.getResourceAsStream("/CacheKeys.xml");

		Document doc = Parser.parseXMLFile(is);

		NodeList nl = doc.getElementsByTagName("ns");
		for (int i = 0; i < nl.getLength(); i++) {
			Element nsNode = (Element) nl.item(i);
			String ns = nsNode.getAttribute("name");

			if (global || ns.equals(domain)) {

				NodeList keys = nsNode.getElementsByTagName("CacheKey");
				int len = keys.getLength();
				for (int j = 0; j < len; j++) {
					Element keyElem = (Element) keys.item(j);
					String id = getAttrEmptyNull(keyElem, "id");
					String key = getAttrEmptyNull(keyElem, "key");
					String ttl = getAttrEmptyNull(keyElem, "ttl");
					String unit = getAttrEmptyNull(keyElem, "unit");
					CacheKey kObj = new CacheKey();
					kObj.setId(id);
					kObj.setKey("cache:" + ns + ":" + key);
					if ("-1".equals(ttl)) {
						kObj.setTtl(-1);
					} else {
						kObj.setTtl(getTTL(ttl, unit));
					}
					keysMap.put(id, kObj);
				}
			}

		}

		// System.out.println(keysMap.get("1002").getKey());
		try {
			is.close();
		} catch (IOException e) {
		}
	}

	/**
	 * d,h,m,s
	 * 
	 * @param ttl
	 * @param unit
	 * @return
	 */
	private static int getTTL(String ttl, String unit) {
		int time = Integer.parseInt(ttl);
		if ("d".equals(unit))
			return time * 3600 * 24;
		else if ("h".equals(unit))
			return time * 3600;
		else if ("m".equals(unit))
			return time * 3600 * 24 * 30;
		else
			return time;
	}

	/**
	 * Returns an attribute value from an XML element, converting "" to null.
	 * 
	 * @param e
	 *            - an XML element
	 * @param a
	 *            - the attribute name in the XML element
	 * @return the attribute value
	 */
	private static String getAttrEmptyNull(Element e, String attr) {
		String val = e.getAttribute(attr);
		return val == null || val.length() == 0 ? null : val;
	}

}
