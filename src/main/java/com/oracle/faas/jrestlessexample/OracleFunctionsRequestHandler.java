package com.oracle.faas.jrestlessexample;

import com.jrestless.core.container.handler.SimpleRequestHandler;
import com.jrestless.core.container.io.DefaultJRestlessContainerRequest;
import com.jrestless.core.container.io.JRestlessContainerRequest;
import com.jrestless.core.container.io.RequestAndBaseUri;
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
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.net.URI;

import static java.util.Objects.requireNonNull;

public abstract class OracleFunctionsRequestHandler extends SimpleRequestHandler<InputEvent, OutputEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(OracleFunctionsRequestHandler.class);
    private String defaultContentType = "applications/json";
    private boolean success = true;


    protected JRestlessContainerRequest createContainerRequest(InputEvent inputEvent) {
        requireNonNull(inputEvent);

        String reqUrl = inputEvent.getRequestUrl();
        String appName = inputEvent.getAppName();

        String base = getBase(reqUrl, appName);
        String path = inputEvent.getRoute();

        URI baseUri = URI.create("/");
        URI requestUri = URI.create(path);

        RequestAndBaseUri requestAndBaseUri = new RequestAndBaseUri(baseUri, requestUri);

        String httpMethod = inputEvent.getMethod();
        InputStream entityStream = returnStream(inputEvent);
        Map<String, List<String>> headers = formatHeaders(inputEvent.getHeaders());

        DefaultJRestlessContainerRequest container = new DefaultJRestlessContainerRequest(requestAndBaseUri, httpMethod, entityStream, headers);

        System.err.println(container.toString());

        return container;
    }

    private String getBase(String fullUrl, String appName){
        if (fullUrl.contains(appName)){
            String[] parts = fullUrl.split(appName);
            return parts[0] + appName;
        } else {
            throw new IllegalArgumentException("The URL " + fullUrl + " does not contain " + appName);
        }
    }

    private Map<String, List<String>> formatHeaders(Map<String, String> jfaasHeaders) {
        Map<String, List<String>> configuredHeaders = new HashMap<>();

        for( String header : jfaasHeaders.keySet() ){
            List<String> headerValues = new ArrayList<>();
            headerValues.add(jfaasHeaders.get(header));
            configuredHeaders.put(header, headerValues);
        }
        return configuredHeaders;
    }

    private InputStream returnStream(InputEvent rawInput) {
        return rawInput.consumeBody(inputStream -> inputStream);
    }


    protected SimpleResponseWriter<OutputEvent> createResponseWriter(@Nonnull InputEvent inputEvent) {
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
            System.err.println("" + statusType);
            System.err.println("" + map);
            String responseBody = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8.name());
            String contentType = map.getOrDefault("Content-Type", Collections.singletonList(defaultContentType)).get(0);
            response = OutputEvent.fromBytes(responseBody.getBytes(), success, contentType + "foo");
        }
    }

    protected OutputEvent onRequestFailure(Exception e, InputEvent inputEvent, @Nullable JRestlessContainerRequest jRestlessContainerRequest) {
        LOG.error("request failed", e);
        System.err.println("request failed" + e.getMessage());
        e.printStackTrace();
        return null;
    }

    public OutputEvent handleRequest(InputEvent inputEvent){
        System.err.println("Here we are");
        OutputEvent outputEvent = this.delegateRequest(inputEvent);
        return outputEvent;
    }


}
