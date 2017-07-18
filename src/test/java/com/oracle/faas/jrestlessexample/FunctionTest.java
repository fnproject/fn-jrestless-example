package com.oracle.faas.jrestlessexample;

import com.oracle.faas.testing.FnTesting;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class FunctionTest {
    @Rule
    public final FnTesting testing = FnTesting.createDefault();

    @Test
    public void shouldReturnText() {
        testing.givenEvent()
                .withRequestUrl("http://localhost:8080/r/myapp/route")
                .withRoute("/route")
                .withAppName("myapp")
                .withMethod("GET")
                .enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals("pong", testing.getOnlyResult().getBodyAsString());
    }

    @Ignore
    @Test
    public void shouldError() {
        testing.givenEvent().withRequestUrl("http://localhost:8080/r/example/entry/testing").enqueue();

        testing.thenRun(ExampleClass.class, "handleRequest");

        Assert.assertEquals(404, testing.getOnlyResult().getStatus());
    }
}
