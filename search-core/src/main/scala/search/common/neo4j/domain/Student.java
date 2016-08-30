
package search.common.neo4j.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;


@NodeEntity
public class Student extends Entity {

    private String name;



    public Student() {
    }

    public Student(String name) {
        this();
        this.name = name;
    }

    public String getName() {
        return name;
    }


}
