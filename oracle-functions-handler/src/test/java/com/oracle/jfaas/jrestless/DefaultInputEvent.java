package com.oracle.jfaas.jrestless;

import com.oracle.faas.api.Headers;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.api.QueryParameters;
import com.oracle.faas.runtime.HeadersImpl;
import com.oracle.faas.runtime.QueryParametersImpl;
import com.oracle.faas.runtime.ReadOnceInputEvent;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultInputEvent {
    private String DOMAIN_WITH_SCHEME = "http://www.example.com";
    private String appName = "myApp";
    private String route = "/route";
    private String requestUrl = DOMAIN_WITH_SCHEME + "/r/route";
    private String method = "GET";
    private InputStream body = new ByteArrayInputStream(new byte[]{});
    private Headers headers = new HeadersImpl(new HashMap<>());
    private QueryParameters parameters = new QueryParametersImpl();

    public DefaultInputEvent(){
    }

    public DefaultInputEvent setReqUrlAndRoute(String reqUrl, String route){
        requestUrl = reqUrl;
        this.route = route;
        return this;
    }

    public DefaultInputEvent setMethod(String method){
        this.method = method;
        return this;
    }

    public DefaultInputEvent setAppName(String name){
        appName = name;
        return this;
    }

    public DefaultInputEvent setBody(InputStream body){
        this.body = body;
        return this;
    }

    public DefaultInputEvent setHeaders(Headers headers){
        this.headers = headers;
        return this;
    }

    public DefaultInputEvent setHeaders(Map<String,String> headers){
        this.headers = new HeadersImpl(headers);
        return this;
    }

    public DefaultInputEvent setQueryParameters(Map<String, List<String>> params){
        parameters = new QueryParametersImpl(params);
        return this;
    }

    public InputEvent getInputEvent(){
        return new ReadOnceInputEvent(appName, route, requestUrl, method, body, headers, parameters);
    }
}
