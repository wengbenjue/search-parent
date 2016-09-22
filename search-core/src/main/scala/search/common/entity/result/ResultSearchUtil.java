package search.common.entity.result;


import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.util.CollectionUtils;
import search.common.entity.news.NewsQuery;
import search.common.entity.news.QueryResult;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by soledede.weng on 2016/9/22.
 */
public class ResultSearchUtil {

    public static void getSuggestList(String suggestQuery, String suggestField, org.elasticsearch.search.suggest.Suggest suggest, QueryResult searchResult) {
        try {
            List<? extends org.elasticsearch.search.suggest.Suggest.Suggestion.Entry<? extends org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option>> list = suggest.getSuggestion(suggestField).getEntries();
            if (list != null && list.size() > 0) {
                Set<String> suggestSet = new HashSet<String>();
                for (int i = 0; i < list.size(); i++) {
                    List<?> options = list.get(i).getOptions();
                    for (int j = 0; j < options.size(); j++) {
                        if (options.get(j) instanceof org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option) {
                            org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option op = (org.elasticsearch.search.suggest.Suggest.Suggestion.Entry.Option) options.get(j);
                            Text text = op.getText();
                            List<String> suggestWords = null;
                            if (text != null) {
                                String suggestText = text.string();
                                if (suggestText != null && !suggestText.trim().equalsIgnoreCase("")) {
                                    suggestWords = CollectionUtils.arrayAsArrayList(suggestText.split(" "));
                                    suggestSet.addAll(suggestWords);
                                }
                            }
                        }
                    }
                }

                if (suggestSet.size() > 0) {
                    List<String> words = new ArrayList<>();
                    suggestSet.forEach(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            words.add(s);
                        }
                    });
                    Map<String, List<String>> spellCheck = new HashMap<String, List<String>>();
                    spellCheck.put(suggestQuery, words);
                    searchResult.setSuggests(spellCheck);
                }
            }

        } catch (Exception e) {

        }
    }
}
