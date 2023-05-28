package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class UrlController {

    // GET /urls?page=value
    public static Handler showAllUrls = ctx -> {
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;
        // rowsOffset

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("url", urls);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };

    // GET /urls/{id}
    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };

    // POST /urls add website
    public static Handler createUrl = ctx -> {
        String urlFull = ctx.formParam("url");

        String constructedUrl = null;

        try {
            URL url = new URL(urlFull);
            String protocol = url.getProtocol();
            String host = url.getHost();
            constructedUrl = protocol + "://" + host;
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
        }

        Url existedUrl = new QUrl()
                .name.equalTo(constructedUrl)
                .findOne();

        if (existedUrl == null) {
            Url newUrl = new Url(constructedUrl);
            newUrl.save();
            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.sessionAttribute("flash-type", "success");
        } else {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls");
    };

    // POST /urls/{id}/checks check for SEO optimization
    public static Handler checkUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();
        try {
            HttpResponse<String> response = Unirest.get(url.getName()).asString(); // return view of the source

            Document document = Jsoup.parse(response.getBody());
            int statusCode = response.getStatus();
            String title = document.title();
            String h1 = getH1(document);
            String description = getDescription(document);
            UrlCheck urlCheck = new UrlCheck(url, statusCode, title, h1, description);
            urlCheck.save();
            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");
        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flash-type", "danger");
        }
        ctx.redirect("/urls/" + id);
    };

    private static String getH1(Element element) {
        Element h1 = element.selectFirst("h1");
        String h1String = h1.text();
        return h1String.isEmpty() ? null : h1String;
    }

    private static String getDescription(Element element) {
        Element description = element.selectFirst("meta[name='description']");
        String descriptionString = description.text();
        return descriptionString.isEmpty() ? null : descriptionString;
    }
}
