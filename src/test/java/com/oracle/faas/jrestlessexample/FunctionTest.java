package com.oracle.faas.jrestlessexample;

import com.oracle.faas.testing.FnTesting;
import org.junit.*;

import java.sql.Connection;
import java.sql.DriverManager;

import static junit.framework.TestCase.assertNotNull;

public class FunctionTest {
    @Rule
    public final FnTesting testing = FnTesting.createDefault();


    //Comment: 0.0.0.0 as test harness means it's being run on a jvm
    // 0.0.0.0 is "this host". Traffic to the mysql port is tunneled to
    // the docker mysql instance for us.
    // IF RUNNING INSIDE DOCKER: the address we'd want to use instead is 172.17.0.1 (port 3306)
    @Before
    public void configure(){
        testing.setConfig("DB_URL", "jdbc:mysql://172.17.0.1/POSTS");
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
                .withRequestUrl("http://localhost:8080/r/myapp/route/")
                .withRoute("/route/")
                .withAppName("myapp")
                .withMethod("GET")
                .withQueryParameter("title", "unknown")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals("{\"date\":null,\"author\":null,\"title\":\"Title Not Found\",\"body\":null}", testing.getOnlyResult().getBodyAsString());
    }


    @Test
    public void testIfConnectionNotNull() {
        Connection connection = null;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            connection = DriverManager.getConnection("jdbc:mysql://172.17.0.1/POSTS", "root", "SgRoV3s");

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
                        "\t\"author\": \"Rae\",\n" +
                        "\t\"date\": \"24/07/17\",\n" +
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
