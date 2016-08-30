package search.common.neo4j.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Service;
import search.common.neo4j.domain.Student;
import search.common.neo4j.repository.StudentRepository;

@Service("studentService")
public class StudentServiceImpl extends GenericService<Student> implements StudentService {

    @Autowired
    private StudentRepository studentRepository;


    @Override
    public GraphRepository<Student> getRepository() {
        return studentRepository;
    }
}
