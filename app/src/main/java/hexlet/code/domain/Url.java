package hexlet.code.domain;


import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Instant;

@Entity
public class Url {

    @Id
    private long id;

    private String name;

    @WhenCreated
    private Instant createdAt;

    public Url(String name) { // use only non-automatically generated fields
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}