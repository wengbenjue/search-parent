
package search.common.neo4j.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import search.common.neo4j.domain.Student;
import search.common.neo4j.service.Service;
import search.common.neo4j.service.StudentService;

@RestController
@RequestMapping(value = "/api/students")
public class StudentController extends Controller<Student> {

    @Autowired
    private StudentService studentService;

    @Override
    public Service<Student> getService() {
        return studentService;
    }

}