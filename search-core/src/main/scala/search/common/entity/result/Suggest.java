package search.common.entity.result;

import java.util.Collection;

/**
 * Created by soledede.weng on 2016/9/22.
 */
public class Suggest {
    private String suggestQuery;
    private Collection<String> text;
    private Float score;
    private Collection<String> highLight;

    public Suggest() {
    }

    public Suggest(String suggestQuery, Collection<String> text, Float score, Collection<String> highLight) {
        this.suggestQuery = suggestQuery;
        this.text = text;
        this.score = score;
        this.highLight = highLight;
    }

    public Suggest(String suggestQuery, Collection<String> text, Float score) {
        this.suggestQuery = suggestQuery;
        this.text = text;
        this.score = score;
    }

    public String getSuggestQuery() {
        return suggestQuery;
    }

    public void setSuggestQuery(String suggestQuery) {
        this.suggestQuery = suggestQuery;
    }

    public Collection<String> getText() {
        return text;
    }

    public void setText(Collection<String> text) {
        this.text = text;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public Collection<String> getHighLight() {
        return highLight;
    }

    public void setHighLight(Collection<String> highLight) {
        this.highLight = highLight;
    }
}
