package search.common.entity.searchinterface;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by soledede on 2016/2/20.
 */
public class FilterAttribute implements Serializable {

    public static Map<String,String> attrIdToattrName = new ConcurrentHashMap<String,String>(); //cache attribuiteId -> attribuiteName


    private String attrId;
    private String attrName;  //eg: 品牌
    private Map<String,Integer> attrValues; //eg:Map(soledede->1,百事达->2)
    private Boolean isRangeValue;  //if is range value eg:

    public FilterAttribute() {
    }

    public FilterAttribute(String attrId, String attrName, Map<String, Integer> attrValues, Boolean isRangeValue) {
        this.attrId = attrId;
        this.attrName = attrName;
        this.attrValues = attrValues;
        this.isRangeValue = isRangeValue;
    }

    public String getAttrId() {
        return attrId;
    }

    public void setAttrId(String attrId) {
        this.attrId = attrId;
    }

    public String getAttrName() {
        return attrName;
    }

    public void setAttrName(String attrName) {
        this.attrName = attrName;
    }

    public static Map<String, String> getAttrIdToattrName() {
        return attrIdToattrName;
    }

    public static void setAttrIdToattrName(Map<String, String> attrIdToattrName) {
        FilterAttribute.attrIdToattrName = attrIdToattrName;
    }

    public Map<String, Integer> getAttrValues() {
        return attrValues;
    }

    public void setAttrValues(Map<String, Integer> attrValues) {
        this.attrValues = attrValues;
    }

    public Boolean getRangeValue() {
        return isRangeValue;
    }

    public void setRangeValue(Boolean rangeValue) {
        isRangeValue = rangeValue;
    }
}
