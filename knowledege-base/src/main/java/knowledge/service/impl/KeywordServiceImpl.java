package knowledge.service.impl;

import knowledge.domain.Keyword;
import knowledge.repository.KeywordRepository;
import knowledge.service.GenericService;
import knowledge.service.KeywordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service("keywordService")
public class KeywordServiceImpl extends GenericService<Keyword> implements KeywordService {

    @Autowired
    private KeywordRepository keywordRepository;


    @Override
    public GraphRepository<Keyword> getRepository() {
        return keywordRepository;
    }

    @Override
    public Keyword findByNameOne(String name) {
        return this.keywordRepository.findOneByName(name);
    }

    @Override
    public List<Keyword> findByName(String name) {
        return this.keywordRepository.findByName(name);
    }

    @Override
    public List<Keyword> synonymyByName(String name) {
        return this.keywordRepository.synonymyByName(name);
    }

    @Override
    public Boolean addSynonymy(String kv, String synonym) {
        return newOUpdateSyn(kv, synonym);
    }

    private Boolean newOUpdateSyn(String origin, String synName) {
        return newOUpdateSyn(origin, synName, null);
    }

    private Boolean newOUpdateSyn(String origin, String synName, Integer state) {
        //Integer state = 0; //0->origin have syns 1->synName have not syns  2->no origin,no syns
        try {
            Keyword kv = findByNameOne(origin);
            if (kv != null || (state != null && state == 1)) {
                Set<String> kvW = new HashSet<>();
                List<Keyword> synonyms = kv.getSynonyms();
                List<Keyword> newSynonyms = new ArrayList<>();
                synonyms.forEach(new Consumer<Keyword>() {
                    @Override
                    public void accept(Keyword keyword) {
                        kvW.add(keyword.getName());
                    }
                });

                Iterator<String> it = kvW.iterator();
                Integer cnt = 0;
                while (it.hasNext()) {
                    Keyword keyword = new Keyword(it.next());
                    if (synonyms.contains(keyword)) {
                        newSynonyms.add(synonyms.get(cnt));
                        cnt++;
                    }
                }

                if (!synonyms.contains(new Keyword(synName))) {
                    newSynonyms.add(new Keyword(synName));
                    state = 1;
                } else {
                    state = null;
                }

                newSynonyms.forEach(new Consumer<Keyword>() {
                    @Override
                    public void accept(Keyword keyword) {
                        keyword.setSynonyms(newSynonyms);
                        createOrUpdate(keyword);
                    }
                });

                if (state != null && state == 1) {
                    newOUpdateSyn(origin, synName, state);
                }


            } else {

                List<Keyword> kvSyns = new ArrayList<>();
                Keyword kv1 = new Keyword(origin);
                Keyword kv2 = new Keyword(synName);
                kvSyns.add(kv2);
                kvSyns.add(kv1);
                kv1.setSynonyms(kvSyns);
                createOrUpdate(kv1);
                state = 1; //from 0 to 1
                newOUpdateSyn(origin, synName, state);

            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
