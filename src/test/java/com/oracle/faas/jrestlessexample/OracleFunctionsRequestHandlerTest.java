package com.oracle.faas.jrestlessexample;

import com.jrestless.core.container.JRestlessHandlerContainer;
import com.jrestless.core.container.io.JRestlessContainerRequest;
import com.oracle.faas.api.Headers;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.runtime.HeadersImpl;
import com.oracle.faas.runtime.QueryParametersImpl;
import com.oracle.faas.runtime.ReadOnceInputEvent;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.asynchttpclient.uri.Uri;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class OracleFunctionsRequestHandlerTest {
    private JRestlessHandlerContainer<JRestlessContainerRequest> container;
    private OracleFunctionsRequestHandler requestHandler;

    @Before
    public void setUp() {
        container = mock(JRestlessHandlerContainer.class);
        requestHandler = new DefaultOracleFunctionsRequestHandler(container);

    }

    @Test
    public void inputEventHttpMethodIsSetInContainer() {
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals("GET", containerRequest.getHttpMethod());
    }

    @Test
    public void inputEventHeadersIsSetInContainer() {
        Headers headers = new HeadersImpl(ImmutableMap.of("key_one", "value_one", "key_two", "value_two"));
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                headers,
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals(ImmutableMap.of("key_one", singletonList("value_one"), "key_two", singletonList("value_two")), containerRequest.getHeaders());
    }

    //TODO: test baseUri when Host is passed in as a header
    @Test
    public void inputEventBaseUriIsSetInContainer() {
        URI baseUri = URI.create("/");
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals(baseUri, containerRequest.getBaseUri());
    }

    @Test
    public void inputEventRequestUriIsSetInContainer() {
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        URI requestUri = URI.create(inputEvent.getRoute());
        assertEquals(requestUri, containerRequest.getRequestUri());
    }

    @Test
    public void inputEventEntityStreamWithBodyIsSetInContainer() {
        String content = "42";
        ByteArrayInputStream body = new ByteArrayInputStream(content.getBytes());
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals(ByteArrayInputStream.class, (containerRequest.getEntityStream()).getClass());
        assertEquals(content, toString((ByteArrayInputStream) (containerRequest.getEntityStream())));
    }

    @Test
    public void inputEventEntityStreamWithNoBodyIsSetInContainer() {
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals(ByteArrayInputStream.class, (containerRequest.getEntityStream()).getClass());
        assertEquals("", toString((ByteArrayInputStream) (containerRequest.getEntityStream())));
    }

    private static class DefaultOracleFunctionsRequestHandler extends OracleFunctionsRequestHandler{
        DefaultOracleFunctionsRequestHandler(JRestlessHandlerContainer<JRestlessContainerRequest> container){
            init(container);
            start();
        }

    }

    public static String toString(ByteArrayInputStream bais) {
        int size = bais.available();
        char[] chars = new char[size];
        byte[] bytes = new byte[size];

        bais.read(bytes, 0, size);
        for (int i = 0; i < size;)
            chars[i] = (char) (bytes[i++] & 0xff);

        return new String(chars);
    }
}
