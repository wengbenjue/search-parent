package com.aug3.storage.redisclient.cache;

public abstract class AbstractDataBuilder<T> {

    /**
     * rewrite this method to build data from database or other storage
     * 
     * @return Object
     */
    public abstract T buildData() throws Exception;

}
