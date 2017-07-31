package com.oracle.faas.jrestlessexample;

import com.oracle.faas.jrestlessexample.BloggingApplication.BloggingApp;
import com.oracle.faas.testing.FnTesting;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

public class BloggingResourcesTest {
    @Rule
    public final FnTesting testing = FnTesting.createDefault();

    @Before
    public void configure() throws URISyntaxException {
        testing.setConfig("DB_URL", String.format("jdbc:h2:mem:test;MODE=MySQL;INIT=runscript from '%s'", Paths.get(getClass().getClassLoader().getResource("dbSetup.sql").toURI()).toAbsolutePath()));
        testing.setConfig("DB_USER", "sa");
        testing.setConfig("DB_PASSWORD", "");
        testing.setConfig("DB_DRIVER", "org.h2.Driver");
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

        Assert.assertEquals("TestingBlogpost added", testing.getOnlyResult().getBodyAsString());
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

        Assert.assertEquals(IOUtils.toString(getClass().getResourceAsStream("/index.html")), testing.getOnlyResult().getBodyAsString());
    }

}
