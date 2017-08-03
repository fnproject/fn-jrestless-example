package com.oracle.faas.jrestlessexample.blog;

import com.oracle.jfaas.jrestless.OracleFeature;
import com.oracle.jfaas.jrestless.OracleFunctionsRequestHandler;
import com.oracle.jfaas.jrestless.OracleFunctionsRequestObjectHandler;
import org.glassfish.jersey.server.ResourceConfig;

public class BloggingApp extends OracleFunctionsRequestObjectHandler {

    public BloggingApp() {

        ResourceConfig config = new ResourceConfig();
        config.packages("com.oracle.faas.jrestlessexample.blog");
        config.register(OracleFeature.class);

        init(config);

        start();
    }
}
