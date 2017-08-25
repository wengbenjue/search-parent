package knowledge.service;


import knowledge.domain.Keyword;

import java.util.List;
import java.util.Set;

public interface KeywordService extends Service<Keyword> {

    List<Keyword> findByName(String name);

    Keyword findByNameOne(String name);

    List<Keyword> synonymyByName(String name);

    Boolean addSynonymy(String kv,String synonym);
}
