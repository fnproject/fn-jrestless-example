package com.oracle.faas.jrestlessexample.BloggingApplication;

import com.jrestless.core.filter.ApplicationPathFilter;
import com.oracle.faas.jrestlessexample.OracleFeature;
import com.oracle.faas.jrestlessexample.OracleFunctionsRequestObjectHandler;
import org.glassfish.jersey.server.ResourceConfig;

public class BloggingApp extends OracleFunctionsRequestObjectHandler {

    public BloggingApp() {

        ResourceConfig config = new ResourceConfig();
        config.packages("com.oracle.faas.jrestlessexample");
        config.register(OracleFeature.class);

        init(config);

        start();
    }

}
