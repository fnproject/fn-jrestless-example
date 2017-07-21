package com.oracle.faas.jrestlessexample;

import com.jrestless.core.container.handler.SimpleRequestHandler;
import com.jrestless.core.container.io.DefaultJRestlessContainerRequest;
import com.jrestless.core.container.io.JRestlessContainerRequest;
import com.jrestless.core.container.io.RequestAndBaseUri;
import com.jrestless.core.util.HeaderUtils;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.api.OutputEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public abstract class OracleFunctionsRequestHandler extends SimpleRequestHandler<OracleFunctionsRequestHandler.WrappedInput, OutputEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(OracleFunctionsRequestHandler.class);
    private String defaultContentType = "applications/json";
    private boolean success = true;

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
                HeaderUtils.expandHeaders(inputEvent.getHeaders()));

        System.err.println(container.toString());

        return container;
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
            WrappedInput wrappedInput = new WrappedInput(inputEvent,inputStream);
            return this.delegateRequest(wrappedInput);
        });
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
