package search.common.entity.biz.topic;

/**
 * 对股票概念的封装
 * Created by soledede.weng on 2017-01-09.
 */
public class TopicEntity {
    private String topic; //概念名
    private Integer pulse; //异动值
    private Integer hot; //热度值
    private Double chg; //概念涨跌幅

    public TopicEntity() {
    }

    public TopicEntity(String topic, Integer pulse, Integer hot, Double chg) {
        this.topic = topic;
        this.pulse = pulse;
        this.hot = hot;
        this.chg = chg;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getPulse() {
        return pulse;
    }

    public void setPulse(Integer pulse) {
        this.pulse = pulse;
    }

    public Integer getHot() {
        return hot;
    }

    public void setHot(Integer hot) {
        this.hot = hot;
    }

    public Double getChg() {
        return chg;
    }

    public void setChg(Double chg) {
        this.chg = chg;
    }
}
