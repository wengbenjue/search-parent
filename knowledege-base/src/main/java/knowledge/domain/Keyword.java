package knowledge.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@NodeEntity(label = "keyword")
public class Keyword extends Entity {


    private String name;

   @Relationship(type = "synonym", direction = "UNDIRECTED")
    private List<Keyword> synonyms;

    public void addSynonyms(String synonym) {
        this.synonyms.add(new Keyword(synonym));
    }

    public Keyword() {
        synonyms =  new ArrayList<>();
    }


    public Keyword(String name) {
        this();
        this.name = name;
    }

    public Keyword(String name, List<Keyword> synonyms) {
        this();
        this.name = name;
        this.synonyms = synonyms;
    }

    public List<Keyword> getSynonyms() {
        return synonyms;
    }

    public void setSynonyms(List<Keyword> synonyms) {
        this.synonyms = synonyms;
    }

    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Keyword keyword = (Keyword) o;

        return name != null ? name.equals(keyword.name) : keyword.name == null;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Student{" +
                "id=" + getId() +
                ", name='" + name + '\'' +
                '}';
    }
}
