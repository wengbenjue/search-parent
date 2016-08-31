package knowledge.controller;

import knowledge.domain.Keyword;
import knowledge.entity.Msg;
import knowledge.service.KeywordService;
import knowledge.service.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
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

    @RequestMapping(value = "/k/{name}", method = RequestMethod.GET)
    public List<Keyword> synonym(@PathVariable String name, final HttpServletResponse response) {
        setHeaders(response);
        return keywordService.synonymyByName(name);
    }

    @RequestMapping(value = "/add/{kv}/{synonym}", method = RequestMethod.GET)
    public Msg addSynonym(@PathVariable String kv, @PathVariable String synonym, final HttpServletResponse response) {
        setHeaders(response);
        Boolean result = keywordService.addSynonymy(kv, synonym);
        if(result)
        return new Msg(0,"synonym added sucessfully");
        else return new Msg(-1,"synonym added failed");
    }

}