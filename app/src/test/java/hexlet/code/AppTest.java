package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Url existingUrl;
    private static Database database;

    public static MockWebServer mockWebServer;

    private static final String MOCK_INDEX_HTML = "mock-index.html";

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();

//        mockWebServer = new MockWebServer();
//        try {
//            MockResponse mockResponse = new MockResponse()
//                    .setBody(Files.readString(MOCK_INDEX_HTML), StandardCharsets.UTF_8)
//        } catch (IOException e) {
//            System.out.println("Could not read file!");
//        }

    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed-test-db.sql");
    }

    @Test
    void testIndex() {
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Анализатор страниц");
    }

    @Nested
    class UrlTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://some-domain.org");
            assertThat(body).contains("https://some-domain.org:8080");
        }

        @Test
        void testShow() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("Проверки");
            assertThat(body).contains("Запустить проверку");
        }

//        @Test
//        void testCheckUrl() {
//            Url urlTest = new Url();
//            urlTest.setName("https://test.org");
//            int statusCode = 200;
//            String title = "Test";
//
//
//
//        }

        @Test
        void testCreate() {
            String inputName = "https://test.org";
            HttpResponse<String> responseUrl = Unirest
                    .post(baseUrl + "/urls")
                    .field("name", inputName)
                    .asEmpty();

            assertThat(responseUrl.getStatus()).isEqualTo(302);
            assertThat(responseUrl.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(inputName);
            assertThat(body).contains("Статья успешно создана");

            Url actualUrl = new QUrl()
                    .name.equalTo(inputName)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(inputName);
        }
    }
}
