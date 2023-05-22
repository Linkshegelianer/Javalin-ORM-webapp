package hexlet.code.domain;

import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import java.time.Instant;

@Entity
public final class UrlCheck {

    @OneToMany
    private Url url;

    @Id
    private long id;

    private int statusCode;

    @Lob
    private String title;

    @Lob
    private String h1;

    @Lob
    private String description;

    @WhenCreated
    private Instant createdAt;

}
