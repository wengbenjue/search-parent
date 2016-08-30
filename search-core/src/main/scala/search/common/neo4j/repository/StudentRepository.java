package search.common.neo4j.repository;

import org.springframework.data.neo4j.repository.GraphRepository;
import search.common.neo4j.domain.Student;

/**
 * Created by soledede.weng on 2016/8/29.
 */
public interface StudentRepository  extends GraphRepository<Student> {
}
