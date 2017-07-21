package com.oracle.faas.jrestlessexample;

import com.jrestless.core.container.dpi.AbstractReferencingBinder;
import com.jrestless.core.container.handler.SimpleRequestHandler;
import com.jrestless.core.container.io.DefaultJRestlessContainerRequest;
import com.jrestless.core.container.io.JRestlessContainerRequest;
import com.jrestless.core.container.io.RequestAndBaseUri;
import com.jrestless.core.util.HeaderUtils;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
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


    protected OracleFunctionsRequestHandler(){

    }


    protected JRestlessContainerRequest createContainerRequest(WrappedInput wrappedInput) {
        InputEvent inputEvent = wrappedInput.inputEvent;
        InputStream entityStream = wrappedInput.stream;

        requireNonNull(inputEvent);

        String path = inputEvent.getRoute();

        URI baseUri = URI.create("/");
        URI requestUri = URI.create(path);

        RequestAndBaseUri requestAndBaseUri = new RequestAndBaseUri(baseUri, requestUri);

        String httpMethod = inputEvent.getMethod();

        DefaultJRestlessContainerRequest container = new DefaultJRestlessContainerRequest(
                requestAndBaseUri,
                httpMethod,
                entityStream,
                HeaderUtils.expandHeaders(inputEvent.getHeaders().getAll()));

        System.err.println(container.toString());

        return container;
    }

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

@Override protected final Binder createBinder() { return new InputBinder(); }

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

        //TODO: Replace the casting in this function
        @Override
        public void writeResponse(@Nonnull Response.StatusType statusType, @Nonnull Map<String, List<String>> map, @Nonnull OutputStream outputStream) throws IOException {
//            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            String responseBody = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8.name());
            String contentType = map.getOrDefault("Content-Type", Collections.singletonList(defaultContentType)).get(0);
            response = OutputEvent.fromBytes(responseBody.getBytes(), success, contentType);
        }
    }

    protected OutputEvent onRequestFailure(Exception e, WrappedInput wrappedInput, @Nullable JRestlessContainerRequest jRestlessContainerRequest) {
        LOG.error("request failed", e);
        System.err.println("request failed" + e.getMessage());
        e.printStackTrace();
        return OutputEvent.emptyResult(false);
    }

    public OutputEvent handleRequest(InputEvent inputEvent){
        return inputEvent.consumeBody((inputStream) -> {
//            System.err.println(ictx.toString());
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
