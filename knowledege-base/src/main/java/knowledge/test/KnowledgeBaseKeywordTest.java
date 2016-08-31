package knowledge.test;

import knowledge.Application;
import knowledge.domain.Keyword;
import knowledge.service.KeywordService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.*;
import java.util.function.Consumer;

/**
 * Created by soledede.weng on 2016/8/30.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
public class KnowledgeBaseKeywordTest {


    @Autowired
    private KeywordService keywordService;

    @Test
    public void getByName() {
        Object result = keywordService.findByName("量子信息");
        System.out.println(result);

        Object resultOne = keywordService.findByNameOne("量子信息");
        System.out.println(resultOne);

    }

    @Test
    public void testAddSynonmy() {
       String origin = "量子信息";
        //String synName = "量子话";
        String synName = "量子信息技术";

        //String origin = "vr";
        //String synName = "虚拟现实";
        newOUpdateSyn(origin, synName, null);

    }


    private void newOUpdateSyn(String origin, String synName, Integer state) {
        //Integer state = 0; //0->origin have syns 1->synName have not syns  2->no origin,no syns

        Keyword kv = keywordService.findByNameOne(origin);
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
                    keywordService.createOrUpdate(keyword);
                }
            });

            if (state!=null && state == 1) {
                newOUpdateSyn(origin, synName, state);
            }


        } else {

            //TODO increment synonmy
            System.out.println("新的词语");
            List<Keyword> kvSyns = new ArrayList<>();
            Keyword kv1 = new Keyword(origin);
            Keyword kv2 = new Keyword(synName);
            kvSyns.add(kv2);
            kvSyns.add(kv1);
            kv1.setSynonyms(kvSyns);
            keywordService.createOrUpdate(kv1);
            state = 1; //from 0 to 1
            newOUpdateSyn(origin, synName, state);

        }
    }


    @Test
    public void saveSynoKeyword() {
        List<Keyword> kv1Syns = new ArrayList<>();
        Keyword kv1 = new Keyword("量子科技1");
        Keyword kv2 = new Keyword("量子信息1");
        Keyword kv3 = new Keyword("量子信息技术1");
        kv1Syns.add(kv2);
        kv1Syns.add(kv3);
        kv1.setSynonyms(kv1Syns);
        Object result = keywordService.createOrUpdate(kv1);


        List<Keyword> kv2Syns = new ArrayList<>();
        kv2Syns.add(kv1);
        kv2Syns.add(kv3);
        kv2.setSynonyms(kv2Syns);
        result = keywordService.createOrUpdate(kv2);

        List<Keyword> kv3Syns = new ArrayList<>();
        kv3Syns.add(kv1);
        kv3Syns.add(kv2);
        kv3.setSynonyms(kv3Syns);


        result = keywordService.createOrUpdate(kv3);

        System.out.println(result);

    }


    @Test
    public void testSynonymyById() {
        Keyword resultOne = keywordService.findByNameOne("量子信息");
        System.out.println(resultOne);
        Object result = keywordService.synonymyByName("量子科技");
        System.out.println(result);
    }

    @Test
    public void delById() {
        Iterable<Keyword> keywords = keywordService.findAll();
        keywords.forEach(new Consumer<Keyword>() {
            @Override
            public void accept(Keyword keyword) {
                keywordService.delete(keyword.getId());
            }
        });


    }
}
