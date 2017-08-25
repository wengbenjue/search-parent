package search.common.mongo;

import java.util.List;

/**
 * Created by soledede on 2016/2/24.
 */
public class MongoException extends Exception {

    private List<Object> list;
    private Exception exceptionInfo;

    public MongoException() {

    }

    public MongoException(List<Object> list, Exception exceptionInfo) {

        this.list = list;
        this.exceptionInfo = exceptionInfo;
    }

    public List<Object> getObjects() {
        return list;
    }
}
