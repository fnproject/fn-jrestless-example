package com.oracle.faas.jrestlessexample;

import org.glassfish.jersey.server.ResourceConfig;

public class ExampleClass extends OracleFunctionsRequestHandler {

    public ExampleClass() {

        ResourceConfig config = new ResourceConfig();
        config.register(BloggingClass.class);
        config.packages("com.oracle.faas.jrestlessexample");

        init(config);

        start();
    }

}
