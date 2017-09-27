package com.fnproject.fn.jrestless;

import com.fnproject.fn.api.*;
import com.jrestless.core.container.dpi.AbstractReferencingBinder;
import com.jrestless.core.container.handler.SimpleRequestHandler;
import com.jrestless.core.container.io.DefaultJRestlessContainerRequest;
import com.jrestless.core.container.io.JRestlessContainerRequest;
import com.jrestless.core.container.io.RequestAndBaseUri;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.server.ContainerRequest;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Fn JRestless Request Handler
 * subclass this and initialise your Jersey app in the constructor
 *
 */
public abstract class FnRequestHandler extends SimpleRequestHandler<FnRequestHandler.WrappedInput, FnRequestHandler.WrappedOutput> {
    private static final Type INPUT_EVENT_TYPE = (new TypeLiteral<Ref<InputEvent>>() { }).getType();
    private static final Type RUNTIME_CONTEXT_TYPE = (new TypeLiteral<Ref<RuntimeContext>>() { }).getType();
    private RuntimeContext rctx;
    private String defaultContentType = MediaType.APPLICATION_JSON;


    /**
     * Creates the JRestlessContainerRequest which is then handled by
     * {@link com.jrestless.core.container.handler delegateRequest}
     */
    @Override
    public JRestlessContainerRequest createContainerRequest(WrappedInput wrappedInput) {
        InputEvent inputEvent = wrappedInput.inputEvent;
        InputStream entityStream = wrappedInput.stream;

        requireNonNull(inputEvent);

        return new DefaultJRestlessContainerRequest(
                getRequestAndBaseUri(inputEvent),
                inputEvent.getMethod(),
                entityStream,
                this.expandHeaders(inputEvent.getHeaders().getAll()));
    }

    private Map<String, List<String>> expandHeaders(Map<String, String> headers) {
        Map<String, List<String>> theHeaders = new HashMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                theHeaders.put(formatKey(e.getKey()), Collections.singletonList(e.getValue()));
            }
        }
        return theHeaders;
    }

    /**
     * Returns the base and request URI for this request.
     * <p>
     *  The base Uri is constructed from the domain and path minus the route
     *  i.e. a request Uri "http://localhost:8080/r/app/route" will have a base Uri "http://localhost:8080/r/app/"
     *
     *  The request Uri is taken from {@code inputEvent.getRequestUrl()}
     *
     * @param inputEvent    The inputEvent given by the functions platform
     * @return the base and request URI for this request
     */
    public RequestAndBaseUri getRequestAndBaseUri(InputEvent inputEvent) {
        URI baseUri;
        String route = inputEvent.getRoute();
        URI requestUri = URI.create(inputEvent.getRequestUrl());
        String path = requestUri.getPath();

        if (!path.endsWith(route)) {
            throw new IllegalStateException("Should have a valid route in the Uri");
        }
        String split = path.substring(0, path.length() - route.length());
        baseUri = UriBuilder.fromUri(requestUri).replacePath(split + "/").replaceQuery(null).build();

        return new RequestAndBaseUri(baseUri, requestUri);
    }

    // Note: The functions platform currently will only give '_' characters so this must be reformatted
    private String formatKey(String key) {
        return key.toLowerCase().replace('_', '-');
    }

    /**
     * Hook that allows you to extend the actual containerRequest passed to the Jersey container
     */
    @Override
    protected void extendActualJerseyContainerRequest(ContainerRequest actualContainerRequest, JRestlessContainerRequest containerRequest, WrappedInput wrappedInput) {
        InputEvent event = wrappedInput.inputEvent;
        actualContainerRequest.setRequestScopedInitializer(locator -> {
            Ref<InputEvent> inputEventRef = locator
                    .<Ref<InputEvent>>getService(INPUT_EVENT_TYPE);
            if (inputEventRef != null) {
                inputEventRef.set(event);
            }
            Ref<RuntimeContext> contextRef = locator
                    .<Ref<RuntimeContext>>getService(RUNTIME_CONTEXT_TYPE);
            if (contextRef != null) {
                contextRef.set(rctx);
            }
        });
    }

    /**
     * Allows for injection binding of InputEvent and RuntimeContext
     *
     * Configure provides binding definitions using the exposed binding
     * methods.
     */
    private static class InputBinder extends AbstractReferencingBinder {

        @Override
        public void configure() {
            bindReferencingFactory(InputEvent.class,
                    ReferencingInputEventFactory.class,
                    new TypeLiteral<Ref<InputEvent>>() { }
                    );
            bindReferencingFactory(RuntimeContext.class,
                    ReferencingRuntimeContextFactory.class,
                    new TypeLiteral<Ref<RuntimeContext>>() { }
                    );
        }
    }

    @Override
    protected final Binder createBinder() {
        return new InputBinder();
    }

    private static class ReferencingInputEventFactory extends ReferencingFactory<InputEvent> {

        @Inject
        ReferencingInputEventFactory(final Provider<Ref<InputEvent>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static class ReferencingRuntimeContextFactory extends ReferencingFactory<RuntimeContext> {

        @Inject
        ReferencingRuntimeContextFactory(final Provider<Ref<RuntimeContext>> referenceFactory) {
            super(referenceFactory);
        }
    }

    /**
     * Creates the response to be passed back to the functions platform
     */
    @Override
    protected SimpleResponseWriter<WrappedOutput> createResponseWriter(WrappedInput wrappedInput) {
        return new ResponseWriter();
    }

    private class ResponseWriter implements SimpleResponseWriter<WrappedOutput> {
        private WrappedOutput response;

        @Override
        public WrappedOutput getResponse() {
            return response;
        }

        @Override
        public ByteArrayOutputStream getEntityOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public void writeResponse(Response.StatusType statusType, Map<String, List<String>> headers, OutputStream outputStream) throws IOException {
            // NOTE: This is a safe cast as it is set to a ByteArrayOutputStream by getEntityOutputStream
            // See JRestlessHandlerContainer class for more details
            String responseBody = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8.name());
            String contentType = headers.getOrDefault("Content-Type", Collections.singletonList(defaultContentType)).get(0);
            Map<String,String> outHeaders = new HashMap<>();
            headers.forEach((k,v)->outHeaders.put(k,v.stream().collect(Collectors.joining(","))));

            OutputEvent outputEvent = OutputEvent.fromBytes(responseBody.getBytes(), statusType.getStatusCode(), contentType, Headers.fromMap(outHeaders));
            response = new WrappedOutput(outputEvent, responseBody, statusType);
        }
    }

    /**
     * Upon request failure an empty output event will be returned to the functions platform as part of
     * the WrappedOutput
     */
    @Override
    protected WrappedOutput onRequestFailure(Exception e, WrappedInput wrappedInput, @Nullable JRestlessContainerRequest jRestlessContainerRequest) {
        System.err.println("request failed" + e.getMessage());
        e.printStackTrace();
        OutputEvent outputEvent = OutputEvent.emptyResult(500);
        return new WrappedOutput(outputEvent, null, Response.Status.INTERNAL_SERVER_ERROR);
    }

    /**
     * This simply allows the FnRequestHandler to access the runtime context so it can later
     * be injected into JAX-RS functions
     *
     * @param rctx      The runtime context passed in from the Fn java  FDK
     */
    @FnConfiguration
    public void setRuntimeContext(RuntimeContext rctx) {
        this.rctx = rctx;
    }

    /**
     * This is the entry point for the functions platform as set in the func.yaml
     *
     * The input event is wrapped up with its input stream
     * The output event is obtained from the wrapped output
     *
     * @param inputEvent        The input event passed in from the functions platform
     * @return The output event to the oracle functions platform
     */
    public final OutputEvent handleRequest(InputEvent inputEvent){
        return inputEvent.consumeBody((inputStream) -> {
            WrappedInput wrappedInput = new WrappedInput(inputEvent, inputStream);
            WrappedOutput response = this.delegateRequest(wrappedInput);
            return response.outputEvent;
        });
    }

    public static class WrappedInput {
        final InputEvent inputEvent;
        final InputStream stream;

        public WrappedInput(InputEvent inputEvent, InputStream stream) {
            this.inputEvent = inputEvent;
            this.stream = stream;
        }
    }

    public static class WrappedOutput {
        public final OutputEvent outputEvent;
        @Deprecated // This is not available to the user yet
        public final String body;
        @Deprecated
        // The functions platform prevents the setting of a non-200 result (or 500 in the case of an external platform error)
        public final int statusCode;

        public WrappedOutput(OutputEvent outputEvent, @Nullable String body, Response.StatusType statusType) {
            requireNonNull(statusType);
            this.statusCode = statusType.getStatusCode();
            this.body = body;
            this.outputEvent = outputEvent;
        }
    }
}