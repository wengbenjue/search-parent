package search.common.entity.searchinterface;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by soledede on 2016/2/20.
 */
public class SearchResult implements Serializable {
    private List<Map<String, Object>> result; // goods list   eg:List(Map(sku->"sdsd))
    private Map<String, Map<String, List<String>>> highlighting; //highlighting list eg:Map(1->Map(title->List("<span class='red_searched_txt'>防护口罩</span>")))
    private Map<String, List<String>> spellChecks;  //misspellingsAndCorrections  eg:Map("防护口罩"->List("防尘口罩"))
    private Msg msg; //eg:
    private Integer total;

    public SearchResult() {
    }

    public SearchResult(List<Map<String, Object>> result, Map<String, Map<String, List<String>>> highlighting, Map<String, List<String>> spellChecks, Msg msg, Integer total) {
        this.result = result;
        this.highlighting = highlighting;
        this.spellChecks = spellChecks;
        this.msg = msg;
        this.total = total;
    }

    public List<Map<String, Object>> getResult() {
        return result;
    }

    public void setResult(List<Map<String, Object>> result) {
        this.result = result;
    }

    public Map<String, Map<String, List<String>>> getHighlighting() {
        return highlighting;
    }

    public void setHighlighting(Map<String, Map<String, List<String>>> highlighting) {
        this.highlighting = highlighting;
    }

    public Map<String, List<String>> getSpellChecks() {
        return spellChecks;
    }

    public void setSpellChecks(Map<String, List<String>> spellChecks) {
        this.spellChecks = spellChecks;
    }

    public Msg getMsg() {
        return msg;
    }

    public void setMsg(Msg msg) {
        this.msg = msg;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}
