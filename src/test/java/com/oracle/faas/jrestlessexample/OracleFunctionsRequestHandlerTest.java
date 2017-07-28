package com.oracle.faas.jrestlessexample;

import com.jrestless.core.container.JRestlessHandlerContainer;
import com.jrestless.core.container.io.JRestlessContainerRequest;
import com.oracle.faas.api.Headers;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.runtime.HeadersImpl;
import com.oracle.faas.runtime.QueryParametersImpl;
import com.oracle.faas.runtime.ReadOnceInputEvent;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

//TODO: Refactor all test names
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

    @Test(expected = NullPointerException.class)
    public void createContainerRequest_NoHttpMethodGiven_ShouldThrowNpe() {
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                null,
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        requestHandler.createContainerRequest(wrappedInput);
    }

    @Test
    public void createContainerRequest_HeadersGiven_ShouldUseHeaders() {
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
        assertEquals(ImmutableMap.of(unMangleKey("key_one"), singletonList("value_one"), unMangleKey("key_two"), singletonList("value_two")), containerRequest.getHeaders());
    }

    @Test
    public void createContainerRequest_NullHeaderKeyGiven_ShouldFilterHeader() {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put(null, "value-one");
        headersMap.put("key-two", "value_two");
        Headers headers = new HeadersImpl(headersMap);
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
        assertEquals(ImmutableMap.of("key-two", singletonList("value_two")), containerRequest.getHeaders());
    }

    @Test
    public void createContainerRequest_NullHeaderValueGiven_ShouldFilterHeader() {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("key-one", null);
        headersMap.put("key-two", "value_two");
        Headers headers = new HeadersImpl(headersMap);
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
        assertEquals(ImmutableMap.of(unMangleKey("key_two"), singletonList("value_two")), containerRequest.getHeaders());
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
    public void inputEventRouteSetInContainerAsRequestUri() {
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
    public void createContainerRequest_OneQueryParamGiven_ShouldUseQueryParamInRequestUri() {
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        Map<String, List<String>> params = ImmutableMap.of("query", Collections.singletonList("params"));
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl(params));
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        URI reqUri = URI.create("/route?query=params");
        assertEquals(reqUri, containerRequest.getRequestUri());
    }

    @Test
    public void createContainerRequest_MultipleQueryParamsGiven_ShouldUseQueryParamInRequestUri() {
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        Map<String, List<String>> query = ImmutableMap.of("query1", Collections.singletonList("param1"), "query2", Collections.singletonList("param2"));
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl(query));
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        URI reqUri = URI.create("/route?query1=param1&query2=param2");
        assertEquals(reqUri, containerRequest.getRequestUri());
    }

    @Test
    public void createContainerRequest_OneQueryParamWithMultipleValues_ShouldDuplicateQueryParamInRequestUri() {
        ByteArrayInputStream body = new ByteArrayInputStream(new byte[]{});
        List<String> params = new ArrayList<>();
        params.add("param1");
        params.add("param2");
        Map<String, List<String>> query = ImmutableMap.of("query", params);
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                body,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl(query));
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, body);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        URI reqUri = URI.create("/route?query=param1&query=param2");
        assertEquals(reqUri, containerRequest.getRequestUri());
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

    private String unMangleKey(String key) {
        return key.toLowerCase().replace('_', '-');
    }
}
