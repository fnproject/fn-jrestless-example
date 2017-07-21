package com.oracle.faas.jrestlessexample;

import com.oracle.faas.testing.FnTesting;
import org.junit.*;

public class FunctionTest {
    @Rule
    public final FnTesting testing = FnTesting.createDefault();

    @Before
    public void configure(){
        testing.setConfig("DB_URL", "jdbc:mysql://10.167.103.215/POSTS");
        testing.setConfig("DB_USER", "root");
        testing.setConfig("DB_PASSWORD", "");
    }

    @Ignore
    @Test
    public void canRetrieveContext(){
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/context")
                .withRoute("/route/context")
                .withAppName("myapp")
                .withMethod("GET")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals("jdbc:mysql://10.167.103.215/POSTS", testing.getOnlyResult().getBodyAsString());
    }

    @Ignore
    @Test
    public void shouldReturnText() {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/unknownTitle")
                .withRoute("/route/unknownTitle")
                .withAppName("myapp")
                .withMethod("GET")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals("{\"date\":null,\"author\":null,\"title\":\"Title Not Found\",\"body\":null}", testing.getOnlyResult().getBodyAsString());
    }

    @Ignore
    @Test
    public void testPost() {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route/add")
                .withBody("{\n" +
                        "\t\"title\": \"Spamalot\",\n" +
                        "\t\"date\": \"something\",\n" +
                        "\t\"author\": \"something\",\n" +
                        "\t\"body\": \"something\"\n" +
                        "}")
                .withAppName("myapp")
                .withRoute("/route/add")
                .withMethod("POST")
                .withHeader("Content-Type", "application/json")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals("Spamalot added", testing.getOnlyResult().getBodyAsString());
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
