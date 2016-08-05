package search.common.entity.bizesinterface;

import search.common.entity.state.ProcessState;

/**
 * Created by soledede.weng on 2016/8/4.
 */
public class Result {
    private ProcessState state;
    private Object obj;

    public Result() {
    }

    public Result(ProcessState state) {
        this.state = state;
    }

    public Result(ProcessState state, Object obj) {
        this.state = state;
        this.obj = obj;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }
}
