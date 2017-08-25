package search.common.entity.bizesinterface;

/**
 * Created by soledede.weng on 2016/9/14.
 */
public class GraphTopic extends AbstractGraphEntity {
    public GraphTopic(String id, String name, Double weight) {
        this.id = id;
        this.name = name;
        this.weight = weight;
    }
}
