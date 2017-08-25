package search.common.mongo.base;

import com.aug3.storage.mongoclient.MongoUtils;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

public class MongoPkgTools {
	
	private static Logger log = Logger.getLogger(MongoPkgTools.class);

	/**
	 * 将entity对象还原成DBObject对象
	 * 
	 * @param entity
	 * @param obj
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static void unpackaging(Object entity, DBObject obj)
			throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		unpackaging(null, entity, obj);
	}

	/**
	 * 将DBObject对象封装成entity对象
	 * 
	 * @param entity
	 * @param obj
	 * 
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	public static void packaging(Object entity, DBObject obj)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, InstantiationException {
		List<ReflectNameChain> rncs = createReflectNameChain(entity);
		for (ReflectNameChain rnc : rncs) {
			try {
				String nameChain = rnc.getNameChain();
				Type fType = rnc.getfType();

				if ("id".equals(nameChain)) {
					nameChain = "_id";
				}

				Object val = MongoUtils.getObjectByFieldNameChain(obj,
						nameChain);
				if (val != null) {
					if (val instanceof BasicDBList) {
						// TODO FIX BUG --------------------
						// 如果属性是数组
						// 新建一个空数组
						List oneEntityList = new ArrayList();
						// list中val的类型
						ParameterizedType pType = (ParameterizedType) fType;
						// val 转 BasicDBObject
						BasicDBList listObject = (BasicDBList) val;
						// 转换list
						packagingList(oneEntityList, listObject, pType, entity);
						// 将填充后的数组set进对象
						rnc.getMethod().invoke(rnc.getObj(), oneEntityList);
						// TODO FIX BUG --------------------
					}

					else if (val instanceof BasicDBObject) {
						Class<?>[] parameterTypes = rnc.getMethod()
								.getParameterTypes();// 参数类型
						Class<?> firstParameterType = parameterTypes[0]; // 第一个参数类型

						if (firstParameterType.equals(Map.class)) {
							// TODO FIX BUG --------------------
							// 如果属性是map
							// 新建一个空map
							Map oneEntityMap = new HashMap();
							// map中val的类型
							ParameterizedType pType = (ParameterizedType) fType;
							// val 转 BasicDBObject
							BasicDBObject mapObject = (BasicDBObject) val;
							// 转换map
							packagingMap(oneEntityMap, mapObject, pType, entity);
							// 将填充后的数组set进对象
							rnc.getMethod().invoke(rnc.getObj(), oneEntityMap);
							// TODO FIX BUG --------------------
						} else if(firstParameterType.equals(LinkedHashMap.class)){
							// TODO FIX BUG --------------------
							// 如果属性是map
							// 新建一个空map
							Map oneEntityMap = new LinkedHashMap();
							// map中val的类型
							ParameterizedType pType = (ParameterizedType) fType;
							// val 转 BasicDBObject
							BasicDBObject mapObject = (BasicDBObject) val;
							// 转换map
							packagingMap(oneEntityMap, mapObject, pType, entity);
							// 将填充后的数组set进对象
							rnc.getMethod().invoke(rnc.getObj(), oneEntityMap);
							// TODO FIX BUG --------------------
						} else {
							Object oneEntity = firstParameterType
									.getDeclaredConstructors()[0]
									.newInstance(entity);
							packaging(oneEntity, (BasicDBObject) val);
							rnc.getMethod().invoke(rnc.getObj(), oneEntity);
						}

					}

					else {
						try {
							Class<?>[] parameterTypes = rnc.getMethod()
									.getParameterTypes();// 参数类型
							Class<?> firstParameterType = parameterTypes[0]; // 第一个参数类型
							// 数据格式容错机制。如果实际数据类型 与 既定数据类型不符。尝试进行数据类型转换
							// 主要的错误发生在Long Integer 和 String。暂定这三种容错转换
							if (!firstParameterType.equals(val.getClass())) {
								if (Long.class.equals(firstParameterType)) {
									val = Double.valueOf(val.toString())
											.longValue();
								} else if (Integer.class
										.equals(firstParameterType)) {
									val = Double.valueOf(val.toString())
											.intValue();
								} else if (String.class
										.equals(firstParameterType)) {
									val = String.valueOf(val.toString());
								} else if (Double.class
										.equals(firstParameterType)) {
									val = Double.valueOf(val.toString());
								}
							}
							rnc.getMethod().invoke(rnc.getObj(), val);
						} catch (Exception e) {
							String hit = "数据转换遇到错误的数据格式 - obj: " + obj
									+ "entity: " + entity;
							log.error(hit, e);
						}
					}
				}
			} catch (Exception e) {
				log.error(e, e);
			}
		}
	}

	/**
	 * 递归完成list的封装
	 * 
	 * @param list
	 *            entity的list
	 * @param obj
	 *            dbobject的list
	 * @param pType
	 *            entity的list的格式
	 * @param entity
	 *            整个entity的对象
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws SecurityException
	 */
	private static void packagingList(List list, Object obj,
			ParameterizedType pType, Object entity)
			throws InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException,
			SecurityException {

		Type t = pType.getActualTypeArguments()[0];

		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) t;
			Type rawType = pt.getRawType();
			if (rawType.equals(List.class)) {
				// list中的value是list
				BasicDBList listObject = (BasicDBList) obj;
				for (int i = 0; i < listObject.size(); i++) {
					BasicDBList data = (BasicDBList) listObject.get(i);
					List innerList = new ArrayList();
					list.add(innerList);
					packagingList(innerList, data, pt, entity);
				}
			} else if (rawType.equals(Map.class)) {
				// list中的value是map
				BasicDBList listObject = (BasicDBList) obj;
				for (int i = 0; i < listObject.size(); i++) {
					BasicDBList data = (BasicDBList) listObject.get(i);
					List innerList = new ArrayList();
					list.add(innerList);
					packagingList(innerList, data, pt, entity);
				}
			}
		} else if (t instanceof Class) {
			Class c = (Class) t;
			boolean isBaseObject = false;
			Class currC = c;
			while (true) {
				currC = currC.getSuperclass();
				if (currC == null) {
					break;
				}
				if (Object.class.equals(currC)) {
					break;
				}
				if (currC.equals(BaseObject.class)) {
					isBaseObject = true;
					break;
				}
			}

			BasicDBList listObject = (BasicDBList) obj;
			for (int i = 0; i < listObject.size(); i++) {
				if (isBaseObject) {
					// list中的value是 ... entity
					Object oneEntity = c.getDeclaredConstructors()[0]
							.newInstance(entity);
					DBObject object = (DBObject) listObject.get(i);
					packaging(oneEntity, object);
					list.add(oneEntity);
				} else {
					// list中的value是 ... 普通属性
					Object object = listObject.get(i);
					list.add(object);
				}
			}
		} else {
			// nothing to do
		}

	}

	/**
	 * 递归完成map的封装
	 * 
	 * @param map
	 *            entity的map
	 * @param obj
	 *            dbobject的map
	 * @param pType
	 *            entity的map的格式
	 * @param entity
	 *            整个entity的对象
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	private static void packagingMap(Map map, Object obj,
			ParameterizedType pType, Object entity)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, InstantiationException {

		Type t = pType.getActualTypeArguments()[1];

		if (t instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) t;
			Type rawType = pt.getRawType();
			if (rawType.equals(List.class)) {
				// map中的value是list
				DBObject mapObject = (DBObject) obj;
				Set<String> keySet = mapObject.keySet();

				for (String oneKey : keySet) {
					BasicDBList data = (BasicDBList) mapObject.get(oneKey);
					List innerList = new ArrayList();
					map.put(oneKey, innerList);
					packagingList(innerList, data, pt, entity);
				}
			} else if (rawType.equals(Map.class)) {
				// map中的value是map
				DBObject mapObject = (DBObject) obj;
				Set<String> keySet = mapObject.keySet();
				for (String oneKey : keySet) {
					DBObject data = (DBObject) mapObject.get(oneKey);
					Map innerMap = new HashMap();
					map.put(oneKey, innerMap);
					packagingMap(innerMap, data, pt, entity);
				}
			}
		} else if (t instanceof Class) {
			Class c = (Class) t;
			boolean isBaseObject = false;
			Class currC = c;
			while (true) {
				currC = currC.getSuperclass();
				if (currC == null) {
					break;
				}
				if (Object.class.equals(currC)) {
					break;
				}
				if (currC.equals(BaseObject.class)) {
					isBaseObject = true;
					break;
				}
			}

			DBObject mapObject = (DBObject) obj;
			Set<String> keySet = mapObject.keySet();
			for (String oneKey : keySet) {
				if (isBaseObject) {
					// map中的value是 ... entity
					Object oneEntity = c.getDeclaredConstructors()[0]
							.newInstance(entity);
					DBObject object = (DBObject) mapObject.get(oneKey);
					packaging(oneEntity, object);
					map.put(oneKey, oneEntity);
				} else {
					// map中的value是 ... 普通属性
					Object object = mapObject.get(oneKey);
					map.put(oneKey, object);
				}
			}

		} else {
			// nothing to do
		}

	}

	/**
	 * 获得对象的名字查询链
	 * 
	 * @param obj
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	private static List<ReflectNameChain> createReflectNameChain(Object obj)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, InstantiationException {
		return createReflectNameChain(obj, "");
	}

	/**
	 * 获得对象的名字查询链
	 * 
	 * @param obj
	 * @param prefix
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws InstantiationException
	 */
	private static List<ReflectNameChain> createReflectNameChain(Object obj,
			String prefix) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException,
			InstantiationException {
		List<ReflectNameChain> result = new ArrayList<ReflectNameChain>();
		// 获得对象类
		Class clz = obj.getClass();
		// 获得内部类
		List<Class> innerClz = Arrays.asList(clz.getDeclaredClasses());
		// System.out.println(innerClz);
		// 获得所有的set方法 (允许1次父类继承)
		Map<String, Method> setMtds = new HashMap<String, Method>();
		Map<String, Method> getMtds = new HashMap<String, Method>();
		Method[] ms = clz.getDeclaredMethods();
		for (Method m : ms) {
			String name = m.getName();
			if (name.startsWith("set")) {
				setMtds.put(name.toLowerCase(), m);
			}
			if (name.startsWith("get")) {
				getMtds.put(name.toLowerCase(), m);
			}
		}
		Method[] superClassMs = clz.getSuperclass().getDeclaredMethods();
		for (Method m : superClassMs) {
			String name = m.getName();
			if (name.startsWith("set")) {
				setMtds.put(name.toLowerCase(), m);
			}
			if (name.startsWith("get")) {
				getMtds.put(name.toLowerCase(), m);
			}
		}

		// 获得属性 (允许1次父类继承)
		Field[] thisClassFs = clz.getDeclaredFields();
		Field[] superClassFs = clz.getSuperclass().getDeclaredFields();
		int len = thisClassFs.length + superClassFs.length;
		Field[] fs = new Field[len];
		int i = 0;
		for (Field f : thisClassFs) {
			fs[i++] = f;
		}
		for (Field f : superClassFs) {
			fs[i++] = f;
		}
		for (Field f : fs) {
			String fName = f.getName();
			Type fType = f.getGenericType();

			// 忽视掉没有set和get方法的属性
			String oneSetMtd = "set" + fName.toLowerCase();
			if (!setMtds.keySet().contains(oneSetMtd)) {
				continue;
			}
			String oneGetMtd = "get" + fName.toLowerCase();
			if (!getMtds.keySet().contains(oneGetMtd)) {
				continue;
			}
			if (innerClz.contains(fType)) {
				Object fieldObj = getMtds.get(oneGetMtd).invoke(obj,
						new Object[] {});
				if (fieldObj == null) {
					fieldObj = ((Class) fType).getDeclaredConstructors()[0]
							.newInstance(obj);
					setMtds.get(oneSetMtd).invoke(obj,
							new Object[] { fieldObj });
				}
				String childPrefix = "";
				if (StringUtils.isBlank(prefix)) {
					childPrefix = fName;
				} else {
					childPrefix = prefix + "." + fName;
				}
				result.addAll(createReflectNameChain(fieldObj, childPrefix));
			} else {
				String nameChain = "";
				if ("".equals(prefix)) {
					nameChain = nameChain + fName;
				} else {
					nameChain = nameChain + prefix + "." + fName;
				}
				// 生产rnc对象
				ReflectNameChain rnc = new ReflectNameChain();
				rnc.setfType(fType);
				rnc.setMethod(setMtds.get(oneSetMtd));
				rnc.setNameChain(nameChain);
				rnc.setObj(obj);
				result.add(rnc);
			}

		}
		return result;
	}

	/**
	 * 将entity对象还原成DBObject对象的底层实现
	 * 
	 * @param key
	 * @param objectV
	 * @param pNode
	 * @return
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private static DBObject unpackaging(String key, Object objectV,
			DBObject pNode) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException {

		if (objectV == null) {
			return pNode;
		}

		if (pNode == null) {
			return pNode;
		}

		if (objectV instanceof String || objectV instanceof Integer
				|| objectV instanceof Double || objectV instanceof Long
				|| objectV instanceof Date || objectV instanceof Boolean
				|| objectV instanceof ObjectId) {
			if (pNode instanceof BasicDBList) {
				((BasicDBList) pNode).add(objectV);
			} else if (pNode instanceof BasicDBObject) {
				pNode.put(key, objectV);
			} else {
				// ...
			}
			return pNode;
		}

		else if (objectV instanceof BaseObject) {
			DBObject currNode = pNode;
			if (key != null) {
				currNode = new BasicDBObject();
				pNode.put(key, currNode);
			} else if (pNode instanceof BasicDBList) {
				currNode = new BasicDBObject();
				((BasicDBList) pNode).add(currNode);
			}
			// 获得对象类
			Class clz = objectV.getClass();

			// 获得属性
			List<Field> fs = getFields(clz);
			Method m = null;
			String methodName = null;
			for (Field f : fs) {
				String fName = f.getName();
				if(StringUtils.equals(fName, "this$1") || StringUtils.equals(fName, "serialVersionUID") ) {
					continue;
				}
				try {
					methodName = getMethodName(fName);
					m = clz.getMethod(methodName);
				} catch (NoSuchMethodException | SecurityException e) {
					log.warn(fName+" get method is not exists");
					continue;
				}
				Object fv = m.invoke(objectV, new Object[] {});
				unpackaging(fName, fv, currNode);
			}
//			// 获得属性
//			List<String> fields = new ArrayList<String>();
//			Field[] fs = clz.getDeclaredFields();
//			for (Field f : fs) {
//				String fName = f.getName();
//				fields.add(fName);
//			}

//			// 获得所有的get方法
//			Method[] ms = clz.getDeclaredMethods();
//			for (Method m : ms) {
//				String name = m.getName();
//				String fn = Character.toLowerCase(name.charAt(3))
//						+ name.substring(4);
//
//				if (!name.startsWith("get")) {
//					continue;
//				}
//				if (!fields.contains(fn)) {
//					continue;
//				}
//
//				Object fv = m.invoke(objectV, new Object[] {});
//				unpackaging(fn, fv, currNode);
//			}
			// 移除掉空对象
			if (currNode.toMap().size() == 0) {
				pNode.removeField(key);
			}
			return pNode;
		}

		else if (objectV instanceof Map) {
			DBObject mapNode = new BasicDBObject();
			if (pNode instanceof BasicDBList) {
				((BasicDBList) pNode).add(mapNode);
			} else if (pNode instanceof BasicDBObject) {
				pNode.put(key, mapNode);
			} else {
				// ...
			}

			Set<String> keySet = ((Map) objectV).keySet();
			for (String mapKey : keySet) {
				Object mapVal = ((Map) objectV).get(mapKey);
				unpackaging(mapKey, mapVal, mapNode);
			}
			return pNode;
		}

		else if (objectV instanceof List) {
			BasicDBList listNode = new BasicDBList();
			if (pNode instanceof BasicDBList) {
				((BasicDBList) pNode).add(listNode);
			} else if (pNode instanceof BasicDBObject) {
				pNode.put(key, listNode);
			} else {
				// ...
			}

			for (Object oInList : (List) objectV) {
				unpackaging(null, oInList, listNode);
			}
			return pNode;
		}

		else {
			return pNode;
		}

	}
	
	private static List<Field> getFields(Class<?> clazz) {
		List<Field> fieldList = new ArrayList<>();
		for(; clazz != Object.class; clazz=clazz.getSuperclass()) {
			Field[] fields = clazz.getDeclaredFields();
			fieldList.addAll(Arrays.asList(fields));
		}
		return fieldList.stream().filter(f -> !needEliminate(f.getName())).collect(Collectors.toList());
	}
	
	private static String getMethodName(String name) {
		return "get".concat(firstUpper(name));
	}
	
	private static String firstUpper(String name) {
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}
	
	private static boolean needEliminate(String name) {
		return StringUtils.equals(name, "serialVersionUID") ||
				StringUtils.equals(name, "this$0");
	}

}
