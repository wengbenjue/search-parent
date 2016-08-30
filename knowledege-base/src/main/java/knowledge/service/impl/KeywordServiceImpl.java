package knowledge.service.impl;

import knowledge.domain.Keyword;
import knowledge.domain.Student;
import knowledge.repository.KeywordRepository;
import knowledge.repository.StudentRepository;
import knowledge.service.GenericService;
import knowledge.service.KeywordService;
import knowledge.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service("keywordService")
public class KeywordServiceImpl extends GenericService<Keyword> implements KeywordService {

    @Autowired
    private KeywordRepository keywordRepository;


    @Override
    public GraphRepository<Keyword> getRepository() {
        return keywordRepository;
    }

    @Override
    public Keyword findByNameOne(String name) {
        return this.keywordRepository.findOneByName(name);
    }

    @Override
    public List<Keyword> findByName(String name) {
        return this.keywordRepository.findByName(name);
    }

    @Override
    public Set<Keyword> synonymyByName(String name) {
        return this.keywordRepository.synonymyByName(name);
    }
}
