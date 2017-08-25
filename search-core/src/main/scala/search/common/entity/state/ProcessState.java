package search.common.entity.state;

import search.common.util.Util;

/**
 * Created by soledede.weng on 2016/8/4.
 */
public class ProcessState {
    private Integer currentState;
    private Integer finished;


    public ProcessState() {
    }

    public ProcessState(Integer currentState, Integer finished) {
        this.currentState = currentState;
        this.finished = finished;
    }

    public Integer getCurrentState() {
        return currentState;
    }

    public void setCurrentState(Integer currentState) {
        this.currentState = currentState;
    }

    public Integer getFinished() {
        return finished;
    }

    public void setFinished(Integer finished) {
        this.finished = finished;
    }

    @Override
    public ProcessState clone() throws CloneNotSupportedException {
        ProcessState state = new ProcessState(this.currentState,this.finished);
        return state;
    }
}
