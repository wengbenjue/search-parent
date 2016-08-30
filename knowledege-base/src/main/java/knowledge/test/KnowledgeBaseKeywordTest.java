package knowledge.test;

import knowledge.Application;
import knowledge.domain.Keyword;
import knowledge.domain.Student;
import knowledge.repository.StudentRepository;
import knowledge.service.KeywordService;
import knowledge.service.StudentService;
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
    private StudentService studentService;

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
    public void getObjById() {
        Object s = studentService.find(63L);
        System.out.println(s);

    }

    @Test
    public void saveSynoKeyword() {
        Keyword kv = new Keyword("量子科技");
        kv.addSynonyms("量子信息");
        kv.addSynonyms("量子信息技术");
        Object result = keywordService.createOrUpdate(kv);
        System.out.println(result);

        kv = new Keyword("大数据");
        kv.addSynonyms("BigData");
        kv.addSynonyms("Data Mining");
        result = keywordService.createOrUpdate(kv);
        System.out.println(result);

    }


    @Test
    public void saveStudents() {
        Student s = new Student("soledede");
        Student s1 = new Student("xiaioa");
        Set<Student> setS = new HashSet<Student>();
        setS.add(s1);
        s.setFriends(setS);
        Object obj = studentService.createOrUpdate(s);
        System.out.println(obj);
    }

    @Test
    public void testSynonymyById(){
        Keyword resultOne = keywordService.findByNameOne("量子信息");
        System.out.println(resultOne);
        Object result = keywordService.synonymyByName("量子科技");
        System.out.println(result);
    }

    @Test
    public  void delById(){
        Iterable<Keyword> keywords = keywordService.findAll();
        keywords.forEach(new Consumer<Keyword>() {
            @Override
            public void accept(Keyword keyword) {
                keywordService.delete(keyword.getId());
            }
        });


    }
}
