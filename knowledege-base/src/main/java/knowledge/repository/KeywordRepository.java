package knowledge.repository;

import knowledge.domain.Keyword;
import knowledge.domain.Student;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface KeywordRepository extends GraphRepository<Keyword> {

    List<Keyword> findByName(String name);

    Keyword findOneByName(String name);


    @Query("MATCH (k:keyword)-[:synonym]->(s:keyword) WHERE k.name ={0} RETURN s LIMIT 5")
    Set<Keyword> synonymyByName(String name);

}
