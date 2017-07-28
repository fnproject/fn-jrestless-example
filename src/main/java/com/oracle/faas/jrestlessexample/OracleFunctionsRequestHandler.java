package com.oracle.faas.jrestlessexample;

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

public abstract class OracleFunctionsRequestHandler extends SimpleRequestHandler<OracleFunctionsRequestHandler.WrappedInput, OutputEvent> {
    private RuntimeContext rctx;
    private static final Logger LOG = LoggerFactory.getLogger(OracleFunctionsRequestHandler.class);
    private String defaultContentType = "applications/json";
    private boolean success = true;
    private static final Type INPUT_EVENT_TYPE = (new TypeLiteral<Ref<InputEvent>>() { }).getType();
    private static final Type RUNTIME_CONTEXT_TYPE = (new TypeLiteral<Ref<RuntimeContext>>() { }).getType();
    private static final URI BASE_ROOT_URI = URI.create("/");

    @Override
    protected JRestlessContainerRequest createContainerRequest(WrappedInput wrappedInput) {
        InputEvent inputEvent = wrappedInput.inputEvent;
        InputStream entityStream = wrappedInput.stream;

        requireNonNull(inputEvent);

        String httpMethod = inputEvent.getMethod();

        RequestAndBaseUri requestAndBaseUri = getRequestAndBaseUri(inputEvent);

        DefaultJRestlessContainerRequest container = new DefaultJRestlessContainerRequest(
                requestAndBaseUri,
                httpMethod,
                entityStream,
                this.expandHeaders(inputEvent.getHeaders().getAll()));

        return container;
    }

    //TODO: Add to when routes aquire the ability to have wildcards e.g. 'String basePath = getBasePathUri(inputEvent)'
    //TODO: Check the 'else' statement has everything required
    //TODO: add a test for checking that 'inputEvent.getRoute()' will continue to be relevant
    //TODO: Test that this doesn't only work locally!!!
    @Nonnull
    protected RequestAndBaseUri getRequestAndBaseUri(@Nonnull InputEvent inputEvent){
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

            baseUriWithoutBasePath = baseUriBuilder.build(new Object[0]);
            baseUri = baseUriBuilder.build(new Object[0]);
        } catch (RuntimeException e) {
            LOG.error("baseUriCreationFailure; using baseUri=/ as fallback", e);
            baseUri = BASE_ROOT_URI;
            baseUriWithoutBasePath = baseUri;
        }

        baseUriBuilder = UriBuilder.fromUri(baseUriWithoutBasePath).path(inputEvent.getRoute());
        addQueryParametersIfAvailable(baseUriBuilder, inputEvent);
        return new RequestAndBaseUri(baseUri, baseUriBuilder.build(new Object[0]));
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
                theHeaders.put(unMangleKey(e.getKey()), Collections.singletonList(e.getValue()));
            }
        }
        return theHeaders;
    }

    private String unMangleKey(String key) {
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
        //TODO: Add "actualContainerRequest.setProperty(..." etc?
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
    protected SimpleResponseWriter<OutputEvent> createResponseWriter(@Nonnull WrappedInput wrappedInput) {
        return new ResponseWriter();
    }

    private class ResponseWriter implements SimpleResponseWriter<OutputEvent> {
        private OutputEvent response;

        @Override
        public OutputEvent getResponse() {
            return response;
        }

        @Override
        public OutputStream getEntityOutputStream() {
            return new ByteArrayOutputStream();
        }

        @Override
        public void writeResponse(@Nonnull Response.StatusType statusType, @Nonnull Map<String, List<String>> map, @Nonnull OutputStream outputStream) throws IOException {
            String responseBody = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8.name());
            String contentType = map.getOrDefault("Content-Type", Collections.singletonList(defaultContentType)).get(0);
            response = OutputEvent.fromBytes(responseBody.getBytes(), success, contentType);
        }
    }

    @Override
    protected OutputEvent onRequestFailure(Exception e, WrappedInput wrappedInput, @Nullable JRestlessContainerRequest jRestlessContainerRequest) {
        LOG.error("request failed", e);
        System.err.println("request failed" + e.getMessage());
        e.printStackTrace();
        return OutputEvent.emptyResult(false);
    }

    public OutputEvent handleRequest(InputEvent inputEvent){
        return inputEvent.consumeBody((inputStream) -> {
            WrappedInput wrappedInput = new WrappedInput(inputEvent, inputStream);
            return this.delegateRequest(wrappedInput);
        });
    }

    @FnConfiguration
    public void setRuntimeContext(RuntimeContext rctx){
        this.rctx = rctx;
    }

    static class WrappedInput {
        final InputEvent inputEvent;
        final InputStream stream;

        WrappedInput(InputEvent inputEvent, InputStream stream) {
            this.inputEvent = inputEvent;
            this.stream = stream;
        }
    }
}
