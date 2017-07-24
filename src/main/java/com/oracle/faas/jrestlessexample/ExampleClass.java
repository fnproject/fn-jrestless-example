package com.oracle.faas.jrestlessexample;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class ExampleClass extends OracleFunctionsRequestHandler {

    public ExampleClass() {

        ResourceConfig config = new ResourceConfig();
        config.packages("com.oracle.faas.jrestlessexample");
        config.register(LoggingFeature.class);

        init(config);

        start();
    }

}
