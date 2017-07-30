package com.oracle.faas.jrestlessexample;

import com.jrestless.core.container.dpi.InstanceBinder;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.api.RuntimeContext;
import com.oracle.faas.runtime.HeadersImpl;
import com.oracle.faas.runtime.QueryParametersImpl;
import com.oracle.faas.runtime.ReadOnceInputEvent;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.util.HashMap;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OracleFunctionsRequestHandlerIntTest {
    private OracleFunctionsRequestHandlerImpl handler;
    private TestService testService;
    private RuntimeContext runtimeContext = mock(RuntimeContext.class);
    private ByteArrayInputStream defaultBody;

    @Before
    public void setUp() {
        testService = mock(TestService.class);
        handler = createAndStartHandler(new ResourceConfig(), testService);
        handler.setRuntimeContext(runtimeContext);
        defaultBody = new ByteArrayInputStream(new byte[]{});
    }

    private OracleFunctionsRequestHandlerImpl createAndStartHandler(ResourceConfig config, TestService testService) {
        Binder binder = new InstanceBinder.Builder().addInstance(testService, TestService.class).build();
        config.register(binder);
        config.register(TestResource.class);
        OracleFunctionsRequestHandlerImpl handler = new OracleFunctionsRequestHandlerImpl();
        handler.init(config);
        handler.start();
        return handler;
    }

    @Test
    public void testRuntimeContextInjection() {
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/",
                "www.example.com",
                "DELETE",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        handler.handleRequest(inputEvent);
        verify(testService).injectRuntimeContext(runtimeContext);
    }

    @Test
    public void testInputEventInjection() {
        InputEvent inputEvent = new ReadOnceInputEvent("myApp",
                "/inject-input-event",
                "www.example.com",
                "PUT",
                defaultBody,
                new HeadersImpl(new HashMap<>()),
                new QueryParametersImpl());
        handler.handleRequest(inputEvent);
        verify(testService).injectInputEvent(same(inputEvent));
    }

    public static class OracleFunctionsRequestHandlerImpl extends OracleFunctionsRequestHandler {
    }

    public interface TestService{
        void injectRuntimeContext(RuntimeContext context);
        void injectInputEvent(InputEvent request);
    }

    @Path("/")
    @Singleton // singleton in order to test proxies
    public static class TestResource {
        @javax.ws.rs.core.Context
        private RuntimeContext runtimeContextMember;

        @javax.ws.rs.core.Context
        private InputEvent inputEventMember;

        private final TestService service;
        private final UriInfo uriInfo;

        @Inject
        public TestResource(TestService service, UriInfo uriInfo) {
            this.service = service;
            this.uriInfo = uriInfo;
        }

        @DELETE
        public Response injectRuntimeContext(@javax.ws.rs.core.Context RuntimeContext runtimeContext) {
            service.injectRuntimeContext(runtimeContext);
            return Response.ok().build();
        }

        @Path("/inject-input-event")
        @PUT
        public Response injectInputEvent(@javax.ws.rs.core.Context InputEvent inputEvent) {
            service.injectInputEvent(inputEvent);
            return Response.ok().build();
        }
    }
}
