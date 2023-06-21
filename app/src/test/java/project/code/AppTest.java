package project.code;

import project.code.domain.Url;
import project.code.domain.UrlCheck;
import project.code.domain.query.QUrl;
import project.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class AppTest {

    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private static MockWebServer mockWebServer;
    private static final String ADDED_URL_FIRST = "https://www.reddit.com";
    private static final String ADDED_URL_SECOND = "https://stackoverflow.com";
    public static final String TEST_HTML_PATH = "src/test/resources/";
    private static final String MOCK_INDEX_HTML = "test.html";

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    @BeforeAll
    public static void beforeAll() throws IOException {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() throws IOException {
        app.stop();
        mockWebServer.shutdown();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed-test-db.sql");
    }

    @Test
    void testRootIndex() {
        HttpResponse<String> responseGet = Unirest.get(baseUrl).asString();
        assertThat(responseGet.getStatus()).isEqualTo(200);
        assertThat(responseGet.getBody()).contains("Анализатор страниц");
        assertThat(responseGet.getBody()).contains("Бесплатно проверяйте сайты на SEO пригодность");
    }

    @Nested
    class UrlTest {
        @Test
        void testCreateUrl() {
            String inputValid = "https://www.example.com";

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputValid)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = responseGet.getBody();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(body).contains(inputValid);
            assertThat(body).contains("Сайты");
            assertThat(body).contains("Страница успешно добавлена");

            Url actualUrl = new QUrl()
                    .name.equalTo(inputValid)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(inputValid);
        }

        @Test
        void testCreateInvalidUrl() {
            String inputInvalid = "invalid_url";

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputInvalid)
                    .asString();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/")
                    .asString();
            String body = responseGet.getBody();

            assertThat(body).contains("Некорректный URL");
        }

        @Test
        void testCreateSameUrl() {
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", ADDED_URL_FIRST)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = responseGet.getBody();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(body).contains(ADDED_URL_FIRST);
            assertThat(body).contains("Страница уже существует");
        }

        @Test
        void testShowUrl() {
            Url actualUrl = new QUrl()
                    .name.equalTo(ADDED_URL_SECOND)
                    .findOne();

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls/" + actualUrl.getId())
                    .asString();

            String body = responseGet.getBody();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(body).contains(ADDED_URL_SECOND);
            assertThat(body).contains("Запустить проверку");
        }

        @Test
        void testShowAllUrls() {
            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = responseGet.getBody();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(body).contains(ADDED_URL_FIRST);
            assertThat(body).contains(ADDED_URL_SECOND);
        }

        @Test
        void createStoreUrlCheck() throws IOException {
            mockWebServer = new MockWebServer();
            mockWebServer.start();

            String pathTestIndex = TEST_HTML_PATH + MOCK_INDEX_HTML;
            String mockHtml = null;
            try {
                Path path = Paths.get(pathTestIndex).toAbsolutePath().normalize();
                mockHtml = Files.readString(path, StandardCharsets.UTF_8);
            } catch (NoSuchFileException e) {
                System.err.println("File does not exist: " + e.getFile());
            }

            MockResponse mockResponse = new MockResponse()
                    .setBody(mockHtml)
                    .setResponseCode(201);
            mockWebServer.enqueue(mockResponse);

            String mockUrl = mockWebServer.url("/").toString().replaceAll("/$", "");

            Unirest
                    .post(baseUrl + "/urls")
                    .field("url", mockUrl)
                    .asEmpty();

            Url actualUrl = new QUrl()
                    .name.equalTo(mockUrl)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(mockUrl);

            Unirest
                    .post(baseUrl + "/urls/" + actualUrl.getId() + "/checks")
                    .asEmpty();

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/" + actualUrl.getId())
                    .asString();

            assertThat(response.getStatus()).isEqualTo(200);

            UrlCheck actualCheckUrl = new QUrlCheck()
                    .url.equalTo(actualUrl)
                    .orderBy()
                    .createdAt.desc()
                    .findOne();

            assertThat(actualCheckUrl).isNotNull();
            assertThat(actualCheckUrl.getStatusCode()).isEqualTo(201);
            assertThat(actualCheckUrl.getTitle()).isEqualTo("Test title");
            assertThat(actualCheckUrl.getH1()).isEqualTo("Test h1 header");
            assertThat(actualCheckUrl.getDescription()).contains("Test description");
        }

        @Test
        void testCheckInvalidUrl() {
            String invalidUrl = "https://.com";

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", invalidUrl)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = responseGet.getBody();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(body).contains(invalidUrl);

            Url actualUrl = new QUrl()
                    .name.equalTo(invalidUrl)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(invalidUrl); // pass

            HttpResponse<String> responsePostCheck = Unirest
                    .post(baseUrl + "/urls/%d/checks".formatted(actualUrl.getId()))
                    .asString();

            UrlCheck urlCheck = new QUrlCheck()
                    .url.id.equalTo(actualUrl.getId())
                    .findOne();

            assertThat(responsePostCheck.getStatus()).isEqualTo(302);

            HttpResponse<String> responseGetCheck = Unirest
                    .get(baseUrl + "/urls/%d".formatted(actualUrl.getId()))
                    .asString();

            assertThat(responseGetCheck.getStatus()).isEqualTo(200);
            assertThat(responseGetCheck.getBody()).contains("Некорректный адрес");
        }
    }
}
