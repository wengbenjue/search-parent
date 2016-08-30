package knowledge.test;

import knowledge.Application;
import knowledge.domain.Keyword;
import knowledge.domain.Student;
import knowledge.service.KeywordService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    public void saveSynoKeyword() {
        Set<Keyword> kv1Syns = new HashSet<>();
        Keyword kv1 = new Keyword("量子科技");
        Keyword kv2 = new Keyword("量子信息");
        Keyword kv3 = new Keyword("量子信息技术");
        kv1Syns.add(kv2);
        kv1Syns.add(kv3);
        kv1.setSynonyms(kv1Syns);
        Object result = keywordService.createOrUpdate(kv1);


        Set<Keyword> kv2Syns = new HashSet<>();
        kv2Syns.add(kv1);
        kv2Syns.add(kv3);
        kv2.setSynonyms(kv2Syns);
        result = keywordService.createOrUpdate(kv2);

        Set<Keyword> kv3Syns = new HashSet<>();
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
