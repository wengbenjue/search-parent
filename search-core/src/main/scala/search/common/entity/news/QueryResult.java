package search.common.entity.news;

import org.apache.commons.collections.map.HashedMap;
import scala.Int;

import java.util.*;

/**
 * Created by soledede.weng on 2016/9/21.
 */
public class QueryResult {
    private Integer count;
    private java.util.Map<String, Object>[] result;
    private Map<String, List<String>> suggests;
    private LinkedHashMap<String, LinkedHashMap<String, Double>> wordCounts; //eg: Map(conpanys->Map(工商银行->19093))
    private Map<String,List<SortStruct>> rvwSorts = new HashedMap(); // 跟上面wordCounts存储的数据一样，只是变成了列表给前端
    private Set<String> hlWords;

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

    public LinkedHashMap<String, LinkedHashMap<String, Double>> getWordCounts() {
        return wordCounts;
    }

    public void setWordCounts(LinkedHashMap<String, LinkedHashMap<String, Double>> wordCounts) {
        this.wordCounts = wordCounts;
    }

    public Set<String> getHlWords() {
        return hlWords;
    }

    public void setHlWords(Set<String> hlWords) {
        this.hlWords = hlWords;
    }

    public Map<String, List<SortStruct>> getRvwSorts() {
        return rvwSorts;
    }

    public void setRvwSorts(Map<String, List<SortStruct>> rvwSorts) {
        this.rvwSorts = rvwSorts;
    }
}
