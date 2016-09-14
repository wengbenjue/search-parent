package search.common.entity.bizesinterface;

import java.util.Collection;
import java.util.List;

/**
 * Created by soledede.weng on 2016/9/14.
 */
public abstract class AbstractGraphEntity {
    protected String id;
    protected String name;
    protected Double weight;
    protected Collection<String> relevantWords;

    public AbstractGraphEntity() {
    }




    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Collection<String> getRelevantWords() {
        return relevantWords;
    }

    public void setRelevantWords(Collection<String> relevantWords) {
        this.relevantWords = relevantWords;
    }
}
