package com.oracle.jfaas.jrestless;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jrestless.core.container.dpi.InstanceBinder;
import com.jrestless.core.filter.ApplicationPathFilter;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.api.RuntimeContext;
import com.oracle.faas.runtime.HeadersImpl;
import com.oracle.faas.runtime.QueryParametersImpl;
import com.oracle.faas.runtime.ReadOnceInputEvent;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class OracleFunctionsFutureRequestHandlerTest {
    private OracleFunctionsTestObjectHandler handler;
    private OracleFunctionsRequestHandlerIntTest.TestService testService;
    private RuntimeContext runtimeContext = mock(RuntimeContext.class);
    private ByteArrayInputStream defaultBody;

    @Before
    public void setUp() {
        testService = mock(OracleFunctionsRequestHandlerIntTest.TestService.class);
        handler = createAndStartHandler(new ResourceConfig(), testService);
        defaultBody = new ByteArrayInputStream(new byte[]{});
    }

    private OracleFunctionsTestObjectHandler createAndStartHandler(ResourceConfig config, OracleFunctionsRequestHandlerIntTest.TestService testService) {
        Binder binder = new InstanceBinder.Builder().addInstance(testService, OracleFunctionsRequestHandlerIntTest.TestService.class).build();
        config.register(binder);
        config.register(OracleFunctionsRequestHandlerIntTest.TestResource.class);
        config.register(ApplicationPathFilter.class);
        config.register(OracleFunctionsRequestHandlerIntTest.SomeCheckedAppExceptionMapper.class);
        config.register(OracleFunctionsRequestHandlerIntTest.SomeUncheckedAppExceptionMapper.class);
        config.register(OracleFunctionsRequestHandlerIntTest.GlobalExceptionMapper.class);
        OracleFunctionsTestObjectHandler handler = new OracleFunctionsTestObjectHandler();
        handler.init(config);
        handler.start();
        handler.setRuntimeContext(runtimeContext);
        return handler;
    }

    // Note: This test is important and though the features used to test it aren't currently available it should
    // still work. Using WrappedInput makes testing this much easier.
    @Test
    public void testRoundTrip() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> inputHeaders = ImmutableMap.of(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON,
                HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        String contents = mapper.writeValueAsString(new OracleFunctionsRequestHandlerIntTest.AnObject("123"));
        ByteArrayInputStream body = new ByteArrayInputStream(contents.getBytes());

        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/round-trip",
                "www.example.com",
                "POST",
                body,
                new HeadersImpl(inputHeaders),
                new QueryParametersImpl());
        OracleFunctionsRequestHandler.WrappedOutput wrappedOutput = handler.handleRequest(inputEvent);

        assertEquals(200, wrappedOutput.statusCode);
        assertEquals(contents, wrappedOutput.body);
    }

    // Note: This is a feature that is yet to be implemented in the Functions platform
    // The Functions platform currently only returns status codes of 200 or 500
    @Test
    public void testIncorrectRoute() {
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/unspecified/route",
                "www.example.com",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());

        OracleFunctionsRequestHandler.WrappedOutput wrappedOutput = handler.handleRequest(inputEvent);
        assertEquals(404, wrappedOutput.statusCode);
    }

    @Test
    public void testSpecificCheckedException() {
        testException("/specific-checked-exception", OracleFunctionsRequestHandlerIntTest.SomeCheckedAppExceptionMapper.class);
    }

    @Test
    public void testSpecificUncheckedException() {
        testException("/specific-unchecked-exception", OracleFunctionsRequestHandlerIntTest.SomeUncheckedAppExceptionMapper.class);
    }

    @Test
    public void testUnspecificCheckedException() {
        testException("/unspecific-checked-exception", OracleFunctionsRequestHandlerIntTest.GlobalExceptionMapper.class);
    }

    @Test
    public void testUnspecificUncheckedException() {
        testException("/unspecific-unchecked-exception", OracleFunctionsRequestHandlerIntTest.GlobalExceptionMapper.class);
    }

    private void testException(String resource, Class<? extends ExceptionMapper<?>> exceptionMapper) {
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                resource,
                "www.example.com",
                "GET",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());

        OracleFunctionsRequestHandler.WrappedOutput wrappedOutput = handler.handleRequest(inputEvent);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), wrappedOutput.statusCode);
        assertEquals(exceptionMapper.getSimpleName(), wrappedOutput.body);
    }
}
