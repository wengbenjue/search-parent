package com.aug3.storage.redisclient.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.binary.Base64;

public class ObjectResolver {

	/**
	 * 将object对象转换成字符串
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	public static String encode(Object obj) throws IOException{
		if(obj == null) {
			return null;
		}
		return encodeBase64(serializeObj(obj));
	}
	
	/**
	 * 将字符串转换成object对象
	 * @param s
	 * @return
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	public static Object decode(String s) throws ClassNotFoundException, IOException {
		if(s == null) {
			return null;
		}
		return deserializeObj(ObjectResolver.decodeBase64(s));
	}
	
	private static byte[] decodeBase64(String s) {
		return Base64.decodeBase64(s);
	}

	private static String encodeBase64(byte[] b) {
		return Base64.encodeBase64String(b);
	}
	
	private static Object deserializeObj(byte[] buf) throws IOException, ClassNotFoundException {
		Object obj = null;
		if (buf != null) {
			ObjectInputStream ois = null;
			try {
				ois = new ObjectInputStream(new ByteArrayInputStream(buf));
				obj = ois.readObject();
			} catch (IOException e) {
				throw e;
			} catch (ClassNotFoundException e) {
				throw e;
			} finally {
				if (ois != null) {
					try {
						ois.close();
					} catch (IOException e) {
						throw e;
					}
				}
			}
		}
		return obj;
	}

	private static byte[] serializeObj(Object obj) throws IOException {
		byte[] buf = null;

		if (obj != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = null;
			try {
				oos = new ObjectOutputStream(baos);
				oos.writeObject(obj);
				buf = baos.toByteArray();
			} catch (IOException e) {
				throw e;
			} finally {
				if (oos != null) {
					try {
						oos.close();
					} catch (IOException e) {
					}
				}
			}
		}
		return buf;
	}
	
}
