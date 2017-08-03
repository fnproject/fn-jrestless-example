package com.oracle.jfaas.jrestless;

import com.jrestless.core.container.dpi.AbstractReferencingBinder;
import com.jrestless.core.container.handler.SimpleRequestHandler;
import com.jrestless.core.container.io.DefaultJRestlessContainerRequest;
import com.jrestless.core.container.io.JRestlessContainerRequest;
import com.jrestless.core.container.io.RequestAndBaseUri;
import com.oracle.faas.api.FnConfiguration;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.api.OutputEvent;
import com.oracle.faas.api.RuntimeContext;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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

import static java.util.Objects.requireNonNull;

public abstract class OracleFunctionsRequestHandler extends SimpleRequestHandler<OracleFunctionsRequestHandler.WrappedInput, OracleFunctionsRequestHandler.WrappedOutput> {
    private static final Type INPUT_EVENT_TYPE = (new TypeLiteral<Ref<InputEvent>>() { }).getType();
    private static final Type RUNTIME_CONTEXT_TYPE = (new TypeLiteral<Ref<RuntimeContext>>() { }).getType();
    private static final URI BASE_ROOT_URI = URI.create("/");
    private static final Logger LOG = LoggerFactory.getLogger(OracleFunctionsRequestHandler.class);
    private RuntimeContext rctx;
    private String defaultContentType = MediaType.APPLICATION_JSON;


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

    @Nonnull
    public RequestAndBaseUri getRequestAndBaseUri(@Nonnull InputEvent inputEvent){
        URI baseUri;
        URI baseUriWithoutBasePath;
        UriBuilder baseUriBuilder;
        try {
            String host = getHost(inputEvent);
            boolean hostPresent = !isBlank(host);
            if(!hostPresent) {
                LOG.warn("No host header available; using baseUri=/ as fallback");
                baseUriBuilder = UriBuilder.fromUri(BASE_ROOT_URI);
            } else {
                baseUriBuilder = UriBuilder.fromUri("https://" + host);
            }

            baseUriWithoutBasePath = baseUriBuilder.build();
            if(hostPresent) {
                baseUriBuilder.path("/");
            }
            baseUri = baseUriBuilder.build();
        } catch (RuntimeException e) {
            LOG.error("baseUriCreationFailure; using baseUri=/ as fallback", e);
            baseUri = BASE_ROOT_URI;
            baseUriWithoutBasePath = baseUri;
        }

        baseUriBuilder = UriBuilder.fromUri(baseUriWithoutBasePath).path(inputEvent.getRoute());
        addQueryParametersIfAvailable(baseUriBuilder, inputEvent);
        return new RequestAndBaseUri(baseUri, baseUriBuilder.build());
    }

    private static boolean isBlank(String s) {
        return (s == null) || s.trim().isEmpty();
    }

    private static String getHost(InputEvent inputEvent){
        Map<String, String> headers = inputEvent.getHeaders().getAll();
        if (headers == null) {
            return null;
        }
        return headers.get("Host");
    }

    private static void addQueryParametersIfAvailable(UriBuilder uriBuilder, InputEvent inputEvent) {
        Map<String, List<String>> queryStrings = inputEvent.getQueryParameters().getAll();
        for (Map.Entry<String, List<String>> queryStringEntry : queryStrings.entrySet()) {
            for (String value : queryStringEntry.getValue()){
                uriBuilder.queryParam(queryStringEntry.getKey(), value);
            }
        }
    }

    private Map<String, List<String>> expandHeaders(Map<String, String> headers) {
        Map<String, List<String>> theHeaders = new HashMap<>();
        for (Map.Entry<String, String> e : headers.entrySet()){
            if(e.getKey() != null && e.getValue() != null){
                theHeaders.put(formatKey(e.getKey()), Collections.singletonList(e.getValue()));
            }
        }
        return theHeaders;
    }

    private String formatKey(String key) {
        return key.toLowerCase().replace('_', '-');
    }

    @Override
    protected void extendActualJerseyContainerRequest(ContainerRequest actualContainerRequest, JRestlessContainerRequest containerRequest, WrappedInput wrappedInput){
        InputEvent event = wrappedInput.inputEvent;
        actualContainerRequest.setRequestScopedInitializer(locator -> {
            Ref<InputEvent> inputEventRef = locator
                    .<Ref<InputEvent>>getService(INPUT_EVENT_TYPE);
            if (inputEventRef != null){
                inputEventRef.set(event);
            } else {
                System.err.println("InputEvent injection will not work");
                LOG.error("InputEvent injection will not work");
            }

            Ref<RuntimeContext> contextRef = locator
                    .<Ref<RuntimeContext>>getService(RUNTIME_CONTEXT_TYPE);
            if (contextRef != null){
                contextRef.set(rctx);
            } else {
                System.err.println("RuntimeContext injection will not work");
                LOG.error("RuntimeContext injection will not work");
            }
        });
    }

    @Override
    protected final Binder createBinder() { return new InputBinder(); }

    private static class InputBinder extends AbstractReferencingBinder {

        @Override
        public void configure(){
            bindReferencingFactory(InputEvent.class, ReferencingInputEventFactory.class, new TypeLiteral<Ref<InputEvent>>() { });
            bindReferencingFactory(RuntimeContext.class, ReferencingRuntimeContextFactory.class, new TypeLiteral<Ref<RuntimeContext>>() {
            });
        }
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

    @Override
    protected SimpleResponseWriter<WrappedOutput> createResponseWriter(@Nonnull WrappedInput wrappedInput) {
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
        public void writeResponse(@Nonnull Response.StatusType statusType, @Nonnull Map<String, List<String>> headers, @Nonnull OutputStream outputStream) throws IOException {
            // NOTE: This is a safe cast as it is set to a ByteArrayOutputStream by getEntityOutputStream
            // See JRestlessHandlerContainer class for more details
            String responseBody = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8.name());
            String contentType = headers.getOrDefault("Content-Type", Collections.singletonList(defaultContentType)).get(0);
            OutputEvent outputEvent = OutputEvent.fromBytes(responseBody.getBytes(), true, contentType);
            response = new WrappedOutput(outputEvent, responseBody, statusType);
        }
    }

    @Override
    protected WrappedOutput onRequestFailure(Exception e, WrappedInput wrappedInput, @Nullable JRestlessContainerRequest jRestlessContainerRequest) {
        System.err.println("request failed" + e.getMessage());
        e.printStackTrace();
        OutputEvent outputEvent = OutputEvent.emptyResult(false);
        return new WrappedOutput(outputEvent, null, Response.Status.INTERNAL_SERVER_ERROR);
    }

    @FnConfiguration
    public void setRuntimeContext(RuntimeContext rctx){
        this.rctx = rctx;
    }

    public OutputEvent handleRequest(InputEvent inputEvent){
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
        @Deprecated // The functions platform prevents the setting of a non-200 result (or 500 in the case of an external platform error)
        public final int statusCode;

        public WrappedOutput(@Nonnull OutputEvent outputEvent, @Nullable String body, @Nonnull Response.StatusType statusType){
            requireNonNull(statusType);
            this.statusCode = statusType.getStatusCode();
            this.body = body;
            this.outputEvent = outputEvent;
        }
    }
}
