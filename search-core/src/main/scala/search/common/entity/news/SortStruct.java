package search.common.entity.news;

/**
 * Created by soledede.weng on 2016-10-31.
 */
public class SortStruct {
    private String name;
    private Double value;

    public SortStruct(String name, Double value) {
        this.name = name;
        this.value = value;
    }

    public SortStruct() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
