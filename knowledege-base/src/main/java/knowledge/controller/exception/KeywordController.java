package knowledge.controller.exception;

import knowledge.controller.Controller;
import knowledge.domain.Keyword;
import knowledge.domain.Student;
import knowledge.service.KeywordService;
import knowledge.service.Service;
import knowledge.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/keywords")
public class KeywordController extends Controller<Keyword> {

    @Autowired
    private KeywordService keywordService;

    @Override
    public Service<Keyword> getService() {
        return keywordService;
    }

}