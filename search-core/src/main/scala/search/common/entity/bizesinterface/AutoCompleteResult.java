package search.common.entity.bizesinterface;

import java.util.Collection;

/**
 * Created by soledede.weng on 2016/9/14.
 */
public class AutoCompleteResult {
    private Collection<CompanyStock> company;
    private Collection<Industry> industry;
    private Collection<GraphEvent> event;
    private Collection<GraphTopic> topic;

    public AutoCompleteResult() {
    }

    public AutoCompleteResult(Collection<CompanyStock> company, Collection<Industry> industry, Collection<GraphEvent> event, Collection<GraphTopic> topic) {
        this.company = company;
        this.industry = industry;
        this.event = event;
        this.topic = topic;
    }

    public Collection<CompanyStock> getCompany() {
        return company;
    }

    public void setCompany(Collection<CompanyStock> company) {
        this.company = company;
    }

    public Collection<Industry> getIndustry() {
        return industry;
    }

    public void setIndustry(Collection<Industry> industry) {
        this.industry = industry;
    }

    public Collection<GraphEvent> getEvent() {
        return event;
    }

    public void setEvent(Collection<GraphEvent> event) {
        this.event = event;
    }

    public Collection<GraphTopic> getTopic() {
        return topic;
    }

    public void setTopic(Collection<GraphTopic> topic) {
        this.topic = topic;
    }
}
