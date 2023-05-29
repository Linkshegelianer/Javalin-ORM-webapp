package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Database database;

    public static MockWebServer mockWebServer;

    public static final String ADDED_URL_FIRST = "https://test-domain.org";
    public static final String ADDED_URL_SECOND = "https://test-domain.org:8080";

    public static final String INVALID_URL = "invalid_url";

    public static String INPUT_URL = "https://input.org";

    public static String TEST_HTML_PATH = "src/test/resources";

    public static final int ADDED_URLS_COUNT = 2;

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();

        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    public static void afterAll() throws IOException {
//        database.script().run("/truncate.sql");
        mockWebServer.shutdown();
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/seed-test-db.sql");
    }

    @Test
    void testRootIndex() { // pass
        HttpResponse<String> response = Unirest.get(baseUrl).asString();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody()).contains("Анализатор страниц");
    }

    @Nested
    class UrlTest {

        @Test
        void testCreateUrl() {

            HttpResponse<String> responseUrl = Unirest
                    .post(baseUrl + "/urls")
                    .field("name", INPUT_URL)
                    .asString();

            assertThat(responseUrl.getStatus()).isEqualTo(302);
            assertThat(responseUrl.getHeaders().getFirst("Location")).isEqualTo("/urls"); // pass

            HttpResponse<String> responseRedirect = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = responseRedirect.getBody();

            assertThat(responseRedirect.getStatus()).isEqualTo(200); // pass
            assertThat(body).contains(INPUT_URL); // breaks
            assertThat(body).contains("Сайты"); // pass, index.html in urls
            assertThat(body).contains("Страница успешно добавлена"); // pass

            Url actualUrl = new QUrl()
                    .name.equalTo(INPUT_URL)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(INPUT_URL);
        }

        @Test
        void testCreateInvalidUrl() {
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", INVALID_URL)
                    .asString();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/")
                    .asString();
            String body = response.getBody();

            assertThat(body).contains("Некорректный URL");
        }

        @Test
        void testCreateSameUrl() {

            HttpResponse<String> responseUrl = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", ADDED_URL_FIRST)
                    .asString();

            assertThat(responseUrl.getStatus()).isEqualTo(302);
            assertThat(responseUrl.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> responseRedirect = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = responseRedirect.getBody();

            assertThat(responseRedirect.getStatus()).isEqualTo(200);
            assertThat(body).contains(ADDED_URL_FIRST);
            assertThat(body).contains("Страница уже существует");
        }

        @Test
        void testShowUrl() { // pass
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/1")
                    .asString();
            String body = response.getBody();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(ADDED_URL_FIRST);
            assertThat(body).contains("Запустить проверку");
        }

        @Test
        void testShowAllUrls() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(ADDED_URL_FIRST);
            assertThat(body).contains(ADDED_URL_SECOND);
        }

        @Test
        void createUrlCheck() throws IOException {
            String testHtml = Files.readString(Paths.get(TEST_HTML_PATH, "test.html"));
            String mockUrl = mockWebServer.url("/").toString();

            HttpResponse<String> responsePostUrl = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", mockUrl)
                    .asString();

            HttpResponse<String> responsePostCheck = Unirest
                    .post(baseUrl + "/urls/%d/checks".formatted(3)) // nextUrlId
                    .asString();

            assertThat(responsePostCheck.getStatus()).isEqualTo(302);
            assertThat(responsePostCheck.getHeaders().getFirst("Location")).isEqualTo("/urls/%d".formatted(3));

            HttpResponse<String> responseGetAfterRedirect = Unirest
                    .get(baseUrl + "/urls/%d".formatted(3))
                    .asString();

            assertThat(responseGetAfterRedirect.getStatus()).isEqualTo(200);
            assertThat(responseGetAfterRedirect.getBody()).contains(String.valueOf(3));
            assertThat(responseGetAfterRedirect.getBody()).contains("201");
            assertThat(responseGetAfterRedirect.getBody()).contains("Test title");
            assertThat(responseGetAfterRedirect.getBody()).contains("Test description");
            assertThat(responseGetAfterRedirect.getBody()).contains("Test h1 header");

            Url actualUrl = new QUrl()
                    .name.equalTo(mockUrl)
                    .findOne();

            UrlCheck actualUrlCheck = new QUrlCheck()
                    .url.equalTo(actualUrl)
                    .findOne();

            assertThat(actualUrlCheck).isNotNull();
            assertThat(actualUrlCheck.getUrl().getName()).isEqualTo(mockUrl);
        }
    }
}
