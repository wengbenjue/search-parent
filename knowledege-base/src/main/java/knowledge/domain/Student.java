package knowledge.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.Set;


@NodeEntity
public class Student extends Entity {

    private String name;

    @Relationship(type = "friend")
    private Set<Student> friends;


    public Student() {
    }

    public Student(String name) {
        this();
        this.name = name;
    }


    public Set<Student> getFriends() {
        return friends;
    }

    public void setFriends(Set<Student> friends) {
        this.friends = friends;
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
