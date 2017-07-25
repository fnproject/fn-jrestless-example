package com.oracle.faas.jrestlessexample;

import com.oracle.faas.testing.FnTesting;
import org.junit.*;

import java.sql.Connection;
import java.sql.DriverManager;

import static junit.framework.TestCase.assertNotNull;

public class FunctionTest {
    @Rule
    public final FnTesting testing = FnTesting.createDefault();

    @Before
    public void configure(){
        testing.setConfig("DB_URL", "jdbc:mysql://10.167.103.215/POSTS");
        testing.setConfig("DB_USER", "root");
        testing.setConfig("DB_PASSWORD", "SgRoV3s");
    }

    @Ignore
    @Test
    public void shouldReturnUnknown() {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/unknownTitle")
                .withRoute("/route/unknownTitle")
                .withAppName("myapp")
                .withMethod("GET")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals("{\"date\":null,\"author\":null,\"title\":\"Title Not Found\",\"body\":null}", testing.getOnlyResult().getBodyAsString());
    }

    //TODO: Mention that '.withQueryParameter' is doing odd things
    @Test
    public void queryParamTest() {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/?title=unknown")
                .withRoute("/route/")
                .withAppName("myapp")
                .withMethod("GET")
//                .withQueryParameter("title", "unknown")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals("{\"date\":null,\"author\":null,\"title\":\"Title Not Found\",\"body\":null}", testing.getOnlyResult().getBodyAsString());
    }

    @Test
    public void testIfConnectionNotNull() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection("jdbc:mysql://10.167.103.215/POSTS", "root", "SgRoV3s");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertNotNull(connection);
    }

    @Test
    public void testPost() {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/add")
                .withBody("{\n" +
                        "\t\"title\": \"TestingBlogpost\",\n" +
                        "\t\"date\": \"24/07/17\",\n" +
                        "\t\"author\": \"Rae\",\n" +
                        "\t\"body\": \"Initial test that the database has this post inserted\"\n" +
                        "}")
                .withAppName("myapp")
                .withRoute("/route/add")
                .withMethod("POST")
                .withHeader("Content-Type", "application/json")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals("TestingBlogpost added", testing.getOnlyResult().getBodyAsString());
    }

    @Ignore
    @Test
    public void shouldError() {
        testing.givenEvent().withRequestUrl("http://localhost:8080/r/example/entry/testing")
                .withRoute("/entry/testing")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals(404, testing.getOnlyResult().getStatus());
    }
}
