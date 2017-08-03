package com.oracle.jfaas.jrestless;

import com.jrestless.core.container.JRestlessHandlerContainer;
import com.jrestless.core.container.handler.SimpleRequestHandler;
import com.jrestless.core.container.io.JRestlessContainerRequest;
import com.jrestless.core.container.io.RequestAndBaseUri;
import com.oracle.faas.api.Headers;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.runtime.HeadersImpl;
import com.oracle.faas.runtime.QueryParametersImpl;
import com.oracle.faas.runtime.ReadOnceInputEvent;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class OracleFunctionsRequestHandlerTest {
    private JRestlessHandlerContainer<JRestlessContainerRequest> container;
    private OracleFunctionsRequestHandler requestHandler;
    private ByteArrayInputStream defaultBody;

    @Before
    public void setUp() {
        container = mock(JRestlessHandlerContainer.class);
        requestHandler = new DefaultOracleFunctionsRequestHandler(container);
        defaultBody = new ByteArrayInputStream(new byte[]{});
    }

    @Test
    public void createContainerRequest_HttpMethodGiven_ShouldGetHttpMethod() {
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, defaultBody);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals("GET", containerRequest.getHttpMethod());
    }

    @Test(expected = NullPointerException.class)
    public void createContainerRequest_NoHttpMethodGiven_ShouldThrowNpe() {
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                null,
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, defaultBody);
        requestHandler.createContainerRequest(wrappedInput);
    }

    @Test
    public void createContainerRequest_HeadersGiven_ShouldUseHeaders() {
        Headers headers = new HeadersImpl(ImmutableMap.of("key_one", "value_one", "key_two", "value_two"));
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                headers,
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, defaultBody);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals(ImmutableMap.of("key-one", singletonList("value_one"), "key-two", singletonList("value_two")), containerRequest.getHeaders());
    }

    @Test
    public void createContainerRequest_NullHeaderKeyGiven_ShouldFilterHeader() {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put(null, "value-one");
        headersMap.put("key-two", "value_two");
        Headers headers = new HeadersImpl(headersMap);
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                headers,
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, defaultBody);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals(ImmutableMap.of("key-two", singletonList("value_two")), containerRequest.getHeaders());
    }

    @Test
    public void createContainerRequest_NullHeaderValueGiven_ShouldFilterHeader() {
        Map<String, String> headersMap = new HashMap<>();
        headersMap.put("key-one", null);
        headersMap.put("key-two", "value_two");
        Headers headers = new HeadersImpl(headersMap);
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                headers,
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, defaultBody);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals(ImmutableMap.of("key-two", singletonList("value_two")), containerRequest.getHeaders());
    }

    @Test
    public void createContainerRequest_BodyGiven_ShouldUseBody() {
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
    public void createContainerRequest_NoBodyGiven_ShouldUseEmptyBais() {
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedInput wrappedInput = new OracleFunctionsRequestHandler.WrappedInput(inputEvent, defaultBody);
        JRestlessContainerRequest containerRequest = requestHandler.createContainerRequest(wrappedInput);
        assertEquals(ByteArrayInputStream.class, (containerRequest.getEntityStream()).getClass());
        assertEquals("", toString((ByteArrayInputStream) (containerRequest.getEntityStream())));
    }

    @Test
    public void getRequestAndBaseUri_FallbackBaseUriUsed_ShouldMakeBasePath() {
        URI baseUri = URI.create("/");
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        RequestAndBaseUri requestAndBaseUri = requestHandler.getRequestAndBaseUri(inputEvent);
        assertEquals(baseUri, requestAndBaseUri.getBaseUri());
    }

    @Test
    public void getRequestAndBaseUri_RouteSetAsRequest_ShouldMakeRouteRequestUri() {
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        RequestAndBaseUri requestAndBaseUri = requestHandler.getRequestAndBaseUri(inputEvent);
        URI requestUri = URI.create(inputEvent.getRoute());
        assertEquals(requestUri, requestAndBaseUri.getRequestUri());
    }

    @Test
    public void getRequestAndBaseUri_OneQueryParamGiven_ShouldUseQueryParamInRequestUri() {
        Map<String, List<String>> params = ImmutableMap.of("query", Collections.singletonList("params"));
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl(params));
        RequestAndBaseUri requestAndBaseUri = requestHandler.getRequestAndBaseUri(inputEvent);
        URI reqUri = URI.create("/route?query=params");
        assertEquals(reqUri, requestAndBaseUri.getRequestUri());
    }

    @Test
    public void getRequestAndBaseUri_MultipleQueryParamsGiven_ShouldUseQueryParamInRequestUri() {
        Map<String, List<String>> query = ImmutableMap.of("query1", Collections.singletonList("param1"), "query2", Collections.singletonList("param2"));
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl(query));
        RequestAndBaseUri requestAndBaseUri = requestHandler.getRequestAndBaseUri(inputEvent);
        URI reqUri = URI.create("/route?query1=param1&query2=param2");
        assertEquals(reqUri, requestAndBaseUri.getRequestUri());
    }

    @Test
    public void getRequestAndBaseUri_OneQueryParamWithMultipleValues_ShouldDuplicateQueryParamInRequestUri() {
        List<String> params = new ArrayList<>();
        params.add("param1");
        params.add("param2");
        Map<String, List<String>> query = ImmutableMap.of("query", params);
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/route",
                "www.example.com/path",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl(query));
        RequestAndBaseUri requestAndBaseUri = requestHandler.getRequestAndBaseUri(inputEvent);
        URI reqUri = URI.create("/route?query=param1&query=param2");
        assertEquals(reqUri, requestAndBaseUri.getRequestUri());
    }

    @Test
    public void testResponseWriter_WithoutContentHeader_ShouldDefaultToApplicationJson() throws IOException {
        Map<String, List<String>> headers = new HashMap<>();
        SimpleRequestHandler.SimpleResponseWriter<OracleFunctionsRequestHandler.WrappedOutput> responseWriter = requestHandler.createResponseWriter(null);
        responseWriter.writeResponse(Response.Status.OK, headers, new ByteArrayOutputStream());
        Assert.assertTrue(responseWriter.getResponse().outputEvent.getContentType().get().equals(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testResponseWriter_WithContentHeader_ShouldUseContentHeaderGiven() throws IOException {
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", Collections.singletonList(MediaType.TEXT_HTML));
        SimpleRequestHandler.SimpleResponseWriter<OracleFunctionsRequestHandler.WrappedOutput> responseWriter = requestHandler.createResponseWriter(null);
        responseWriter.writeResponse(Response.Status.OK, headers, new ByteArrayOutputStream());
        Assert.assertTrue(responseWriter.getResponse().outputEvent.getContentType().get().equals(MediaType.TEXT_HTML));
    }

    private static class DefaultOracleFunctionsRequestHandler extends OracleFunctionsRequestHandler{
        DefaultOracleFunctionsRequestHandler(JRestlessHandlerContainer<JRestlessContainerRequest> container){
            init(container);
            start();
        }

    }

    private static String toString(ByteArrayInputStream bais) {
        int size = bais.available();
        char[] chars = new char[size];
        byte[] bytes = new byte[size];

        bais.read(bytes, 0, size);
        for (int i = 0; i < size;)
            chars[i] = (char) (bytes[i++] & 0xff);

        return new String(chars);
    }
}
