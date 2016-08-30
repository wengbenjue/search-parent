package knowledge.controller;

import knowledge.domain.Keyword;
import knowledge.domain.Student;
import knowledge.service.KeywordService;
import knowledge.service.Service;
import knowledge.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Set;

@RestController
@RequestMapping(value = "/api/synonym")
public class KeywordController extends Controller<Keyword> {

    @Autowired
    private KeywordService keywordService;

    @Override
    public Service<Keyword> getService() {
        return keywordService;
    }

    @RequestMapping(value = "/{name}", method = RequestMethod.GET)
    public Set<Keyword> find(@PathVariable String name, final HttpServletResponse response) {
        setHeaders(response);
        return keywordService.synonymyByName(name);
    }

}