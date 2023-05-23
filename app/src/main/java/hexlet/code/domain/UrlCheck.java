package hexlet.code.domain;

import io.ebean.Model;
import io.ebean.annotation.WhenCreated;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import java.time.Instant;

@Entity
public final class UrlCheck extends Model {

    @ManyToOne
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

    public UrlCheck(Url url, int statusCode, String title, String h1, String description) {
        this.url = url;
        this.statusCode = statusCode;
        this.title = title;
        this.h1 = h1;
        this.description = description;
    }

    public Url getUrl() {
        return url;
    }

    public long getId() {
        return id;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getTitle() {
        return title;
    }

    public String getH1() {
        return h1;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
