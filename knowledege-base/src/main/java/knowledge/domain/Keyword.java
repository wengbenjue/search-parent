package knowledge.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;


@NodeEntity(label = "keyword")
public class Keyword extends Entity {

    private String name;

    //@Relationship(type = "synonym", direction = "UNDIRECTED")
    @Relationship(type = "synonym")
    private Set<Keyword> synonyms;

    public void addSynonyms(String synonym) {
        this.synonyms.add(new Keyword(synonym));
    }

    public Keyword() {
        synonyms =  new HashSet<>();
    }


    public Keyword(String name) {
        this();
        this.name = name;
    }

    public Keyword(String name, Set<Keyword> synonyms) {
        this.name = name;
        this.synonyms = synonyms;
    }

    public Set<Keyword> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(Set<Keyword> synonyms) {
        this.synonyms = synonyms;
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    @Override
    public String toString() {
        return "Student{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                '}';
    }
}
