package com.example.fnjrestless.blog;

import com.fnproject.fn.testing.FnResult;
import com.fnproject.fn.testing.FnTestingRule;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class BloggingResourcesTest {
    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Before
    public void configure() throws URISyntaxException {
        testing.setConfig("DB_URL", String.format("jdbc:h2:mem:test;MODE=MySQL;INIT=runscript from '%s'", Paths.get(getClass().getClassLoader().getResource("dbSetup.sql").toURI()).toAbsolutePath()));
        testing.setConfig("DB_USER", "sa");
        testing.setConfig("DB_PASSWORD", "");
        testing.setConfig("DB_DRIVER", "org.h2.Driver");
        testing.addSharedClassPrefix("");
    }

    @Test
    public void postingNewBlogPost() {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/add")
                .withBody("{\n" +
                        "\t\"title\": \"TestingBlogpost\",\n" +
                        "\t\"author\": \"Rae\",\n" +
                        "\t\"date\": \"24/07/17\",\n" +
                        "\t\"body\": \"Initial test that the database has this post inserted\"\n" +
                        "}")
                .withAppName("myapp")
                .withRoute("/route/add")
                .withMethod("POST")
                .withHeader("Content-Type", "application/json")
                .enqueue();

        testing.thenRun(BloggingApp.class, "handleRequest");

        FnResult result = testing.getOnlyResult();

        Assert.assertEquals(200,result.getStatus());
        Assert.assertEquals("TestingBlogpost added", result.getBodyAsString());
    }

    @Test
    public void gettingAllPosts() {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/blogs")
                .withAppName("myapp")
                .withRoute("/route/blogs")
                .withMethod("GET")
                .enqueue();

        testing.thenRun(BloggingApp.class, "handleRequest");

        Assert.assertEquals("[{\"date\":\"Friday\",\"author\":\"Rae\",\"title\":\"Testing\",\"body\":\"Data to retrieve\"}]", testing.getOnlyResult().getBodyAsString());
    }

    @Test
    public void gettingHTMLOfPosts() throws IOException {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/html")
                .withAppName("myapp")
                .withRoute("/route/html")
                .withMethod("GET")
                .enqueue();

        testing.thenRun(BloggingApp.class, "handleRequest");
        InputStream indexHtml = getClass().getResourceAsStream("/index.html");
        String html = IOUtils.toString(indexHtml,StandardCharsets.UTF_8);

        Assert.assertEquals(html, testing.getOnlyResult().getBodyAsString());
    }

}
