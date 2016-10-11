package search.common.entity.news;

import scala.Int;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by soledede.weng on 2016/9/21.
 */
public class QueryResult {
    private Integer count;
    private java.util.Map<String, Object>[] result;
    private Map<String, List<String>> suggests;
    private LinkedHashMap<String, LinkedHashMap<String, Long>> wordCounts; //eg: Map(conpanys->Map(工商银行->19093))

    public QueryResult() {
    }

    public QueryResult(Integer count, Map<String, Object>[] result) {
        this.count = count;
        this.result = result;
    }

    public QueryResult(Integer count, Map<String, Object>[] result, Map<String, List<String>> suggests) {
        this.count = count;
        this.result = result;
        this.suggests = suggests;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Map<String, Object>[] getResult() {
        return result;
    }

    public void setResult(Map<String, Object>[] result) {
        this.result = result;
    }

    public Map<String, List<String>> getSuggests() {
        return suggests;
    }

    public void setSuggests(Map<String, List<String>> suggests) {
        this.suggests = suggests;
    }

    public LinkedHashMap<String, LinkedHashMap<String, Long>> getWordCounts() {
        return wordCounts;
    }

    public void setWordCounts(LinkedHashMap<String, LinkedHashMap<String, Long>> wordCounts) {
        this.wordCounts = wordCounts;
    }
}
