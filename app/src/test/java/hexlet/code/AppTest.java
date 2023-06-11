package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
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
    public static final int ADDED_URLS_COUNT = 3;
    private static final int INVALID_URL_ID = 3;

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

        mockWebServer = new MockWebServer();
        mockWebServer.start();
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
        void testCreateSameUrl() { // pass
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
        void testShowUrl() { // pass
            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls/2")
                    .asString();

            String body = responseGet.getBody();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(body).contains(ADDED_URL_SECOND);
            assertThat(body).contains("Запустить проверку");
        }

        @Test
        void testShowAllUrls() { // pass
            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String body = responseGet.getBody();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(body).contains(ADDED_URL_FIRST);
            assertThat(body).contains(ADDED_URL_SECOND);
        }

        @Test
        void createUrlCheck() throws IOException { // pass
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
            // remove trailing slash
            String mockUrl = mockWebServer.url("/").toString().replaceAll("/$", "");

            int newUrlId = ADDED_URLS_COUNT + 1;

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", mockUrl)
                    .asString();

            HttpResponse<String> responsePostCheck = Unirest
                    .post(baseUrl + "/urls/%d/checks".formatted(newUrlId)) // nextUrlId
                    .asString();

            assertThat(responsePostCheck.getStatus()).isEqualTo(302);
            assertThat(responsePostCheck.getHeaders().getFirst("Location")).isEqualTo("/urls/%d".formatted(newUrlId));

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls/%d".formatted(newUrlId))
                    .asString();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(responseGet.getBody()).contains(String.valueOf(newUrlId));
            assertThat(responseGet.getBody()).contains("201");
            assertThat(responseGet.getBody()).contains("Test title");
            assertThat(responseGet.getBody()).contains("Test description");
            assertThat(responseGet.getBody()).contains("Test h1 header");

            Url url = new QUrl()
                    .name.equalTo(mockUrl)
                    .findOne();

            UrlCheck urlCheck = new QUrlCheck()
                    .url.equalTo(url)
                    .findOne();

            assertThat(urlCheck).isNotNull();
            assertThat(urlCheck.getUrl().getName()).isEqualTo(mockUrl);
        }

        @Test
        void testCheckInvalidUrl() { // pass
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls/%d/checks".formatted(INVALID_URL_ID))
                    .asString();

            UrlCheck urlCheck = new QUrlCheck()
                    .url.id.equalTo(INVALID_URL_ID)
                    .findOne();

            assertThat(responsePost.getStatus()).isEqualTo(302);

            HttpResponse<String> responseGet = Unirest
                    .get(baseUrl + "/urls/%d".formatted(INVALID_URL_ID))
                    .asString();

            assertThat(responseGet.getStatus()).isEqualTo(200);
            assertThat(responseGet.getBody()).contains("Некорректный адрес");
        }
    }
}
