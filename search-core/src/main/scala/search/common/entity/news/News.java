package search.common.entity.news;


import java.util.Collection;
import java.util.Date;

/**
 * Created by soledede.weng on 2016/9/20.
 */
public class News {
    private String id;
    private String title; //标题
    private String auth; //来源
    private String summary; //摘要
    private Date createOn; //创建时间
    private Integer popularity;//热度
    private String url; //新闻url
    private Collection<String> companys; //相关公司
    private Collection<String> events; //相关事件
    private Collection<String> topics; //相关主题

    public News() {
    }

    public News(String id, String title, String auth, String summary, Date createOn, Integer popularity, String url, Collection<String> companys, Collection<String> events, Collection<String> topics) {
        this.id = id;
        this.title = title;
        this.auth = auth;
        this.summary = summary;
        this.createOn = createOn;
        this.popularity = popularity;
        this.url = url;
        this.companys = companys;
        this.events = events;
        this.topics = topics;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuth() {
        return auth;
    }

    public void setAuth(String auth) {
        this.auth = auth;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Date getCreateOn() {
        return createOn;
    }

    public void setCreateOn(Date createOn) {
        this.createOn = createOn;
    }

    public Integer getPopularity() {
        return popularity;
    }

    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }

    public Collection<String> getCompanys() {
        return companys;
    }

    public void setCompanys(Collection<String> companys) {
        this.companys = companys;
    }

    public Collection<String> getEvents() {
        return events;
    }

    public void setEvents(Collection<String> events) {
        this.events = events;
    }

    public Collection<String> getTopics() {
        return topics;
    }

    public void setTopics(Collection<String> topics) {
        this.topics = topics;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
