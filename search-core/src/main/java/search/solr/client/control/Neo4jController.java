package search.solr.client.control;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import search.common.entity.bizesinterface.IndexObjEntity;
import search.common.entity.searchinterface.NiNi;
import search.common.entity.searchinterface.parameter.IndexKeywordsParameter;
import search.common.entity.searchinterface.parameter.KnowledgeGraphParameter;
import search.common.neo4j.controller.Controller;
import search.common.neo4j.domain.Student;
import search.common.neo4j.service.Service;
import search.common.neo4j.service.StudentService;
import search.es.client.biz.BizeEsInterface;
import search.solr.client.searchInterface.SearchInterface;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("/neo")
public class Neo4jController extends Controller<Student> {
    //@Autowired
    private StudentService studentService;

    @Override
    public Service<Student> getService() {
        return studentService;
    }

    //search and filter by keywords
    @RequestMapping(value = "/test", method = {RequestMethod.POST, RequestMethod.GET})
    public NiNi searchByKeywords(final KnowledgeGraphParameter knowledgeGraphParameter) {
       return new NiNi("coming...");
    }

}