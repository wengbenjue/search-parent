package knowledge.service.impl;

import knowledge.domain.Student;
import knowledge.repository.StudentRepository;
import knowledge.service.GenericService;
import knowledge.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Service;

@Service("studentService")
public class StudentServiceImpl extends GenericService<Student> implements StudentService {

    @Autowired
    private StudentRepository studentRepository;


    @Override
    public GraphRepository<Student> getRepository() {
        return studentRepository;
    }
}
