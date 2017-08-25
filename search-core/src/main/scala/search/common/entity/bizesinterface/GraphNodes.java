package search.common.entity.bizesinterface;

import java.util.List;

/**
 * Created by soledede.weng on 2016/9/12.
 */
public class GraphNodes {
    private List message;

    public GraphNodes() {
    }

    public GraphNodes(List message) {
        this.message = message;
    }

    public List getMessage() {
        return message;
    }

    public void setMessage(List message) {
        this.message = message;
    }
}
