package search.common.mongo.base;

import com.aug3.storage.mongoclient.MongoAdaptor;
import com.mongodb.*;
import com.mongodb.client.MongoCursor;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Pattern;

//import com.csf.etlinger.entity.BaseObject;

public abstract class MongoBase {

    protected Logger log = Logger.getLogger(getClass());

    /**
     * 将entity对象解封成DBObject对象
     *
     * @param entity
     * @return
     */
    protected DBObject unpackaging(BaseObject entity) {
        DBObject result = new BasicDBObject();
        try {
            MongoPkgTools.unpackaging(entity, result);
            return result;
        } catch (Exception e) {
            log.error(e, e);
            return result;
        }
    }

    /**
     * 将DBObject对象封装成entity对象
     *
     * @param obj
     * @param persistentClass
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    protected <T> T packaging(DBObject obj, Class<T> persistentClass) {
        try {
            T entity = persistentClass.newInstance();
            MongoPkgTools.packaging(entity, obj);
            return entity;
        } catch (Exception e) {
            log.error(e, e);
            return null;
        }
    }

    /**
     * 将DBObject数组封装成entity数组
     *
     * @param objects
     * @param persistentClass
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public <T> List<T> packaging(List<DBObject> objects, Class<T> persistentClass) {
        List<T> result = new ArrayList<T>();
        if (objects == null || objects.size() == 0) {
            return result;
        }
        for (DBObject obj : objects) {
            T entity = packaging(obj, persistentClass);
            if (entity == null) {
                continue;
            }
            result.add(entity);
        }
        return result;
    }

    /**
     * 获得collection对象
     *
     * @param collectionName ie:base_stock  or ada.base_stock or MongoCollections.COLL_BASE_SHARE_VARY
     * @return
     */
    protected DBCollection getCollection(String collectionName) {
        return MongoAdaptor.getCollection(collectionName);
    }
    protected abstract DBCollection getCollection();

    /**
     * 构造一个可复用的查询语句，$in查询
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    protected BasicDBObject queryIn(Collection collection) {
        return new BasicDBObject("$in", collection);
    }

    @SuppressWarnings("rawtypes")
    protected BasicDBObject queryNotIn(Collection collection) {
        return new BasicDBObject("$nin", collection);
    }

    /**
     * 构造一个可复用的查询语句，表示选择对应的key字段的范围
     * 该方法采取左闭右开的原则
     *
     * @param begin 开始,可以为null
     * @param end   结束,可以为null
     * @return
     */
    protected BasicDBObject queryRange(Object begin, Object end) {
        BasicDBObject basicDBObject = new BasicDBObject();
        if (begin == null && end == null) {
            basicDBObject.put("$exists", true);
        }
        if (begin != null) {
            basicDBObject.put("$gte", begin);
        }
        if (end != null) {
            basicDBObject.put("$lt", end);
        }
        return basicDBObject;
    }

    protected BasicDBObject queryBetween(String begin, String end) {
        BasicDBObject q = new BasicDBObject();
        q.put("$gte", begin);
        q.put("$lte", end);
        return q;
    }

    /**
     * 大于等于 查询
     *
     * @param val
     * @return
     */
    protected DBObject queryGreaterEqThan(Object val) {
        DBObject result = new BasicDBObject();
        result.put("$gte", val);
        return result;
    }

    /**
     * 小于等于
     *
     * @param val
     * @return
     */
    protected DBObject queryLessEqThan(Object val) {
        DBObject result = new BasicDBObject();
        result.put("$lte", val);
        return result;
    }

    /**
     * 大于 查询
     *
     * @param val
     * @return
     */
    protected DBObject queryGreaterThan(Object val) {
        DBObject result = new BasicDBObject();
        result.put("$gt", val);
        return result;
    }

    /**
     * 不等于
     *
     * @param val
     * @return
     */
    protected DBObject queryNotEq(Object val) {
        DBObject result = new BasicDBObject();
        result.put("$ne", val);
        return result;
    }

    /**
     * 小于
     *
     * @param val
     * @return
     */
    protected DBObject queryLessThan(Object val) {
        DBObject result = new BasicDBObject();
        result.put("$lt", val);
        return result;
    }

    /**
     * 构造一个可复用的查询语句，表示对应的key字段字段存在且不为null和不为""
     *
     * @return
     */
    protected DBObject queryNotEmpty() {
        return new BasicDBObject().
                append("$exists", true).append("$nin", Arrays.asList(new String[]{"", "null", null}));
    }

    protected DBObject queryEmpty() {
        return new BasicDBObject().append("$in", Arrays.asList(new String[]{"", "null", null}));
    }

    public Integer count() {
        return count(null);
    }

    /**
     * @param query
     * @return
     */
    public Integer count( DBObject query) {
        DBCollection collection = getCollection();
        Integer cnt = null;
        if (query == null) cnt = collection.find().count();
        else cnt = collection.find(query).count();

        return cnt;
    }

    public List<DBObject>  queryByPage(int start, int pageSize) {
        return queryByPage(null, start, pageSize);
    }

    /**
     * @param query
     * @param start
     * @param pageSize
     * @return
     */
    public List<DBObject> queryByPage( DBObject query, int start, int pageSize) {
        DBCursor cur = null;
        BasicDBObject fields = new BasicDBObject();
        fields.put("_id", 1);
        fields.put("keyWord", 1);
        DBCollection collection =  getCollection();
        if (query == null) cur = collection.find().skip(start).limit(pageSize);
        else cur = collection.find(query).skip(start).limit(pageSize);
        return cur.toArray();
    }

    protected DBObject generateKey(String[] keys) {
        DBObject k = new BasicDBObject();
        if (keys != null) {
            for (String key : keys) {
                k.put(key, 1);
            }
        }
        return k;
    }

    protected Pattern queryRegex(String regex) {
        return Pattern.compile(regex);
    }

    protected void setStat(DBCollection collection, BasicDBObject q, Integer stat) {
        if (q == null) {
            q = new BasicDBObject();
        }
        BasicDBObject o = new BasicDBObject();
        o.append("$set", new BasicDBObject("stat", stat));
        collection.updateMulti(q, o);
    }
}
