package com.oracle.faas.jrestlessexample;

import org.glassfish.jersey.server.ResourceConfig;

import java.awt.event.InputEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class ExampleClass extends OracleFunctionsRequestHandler {

    public ExampleClass(){

        ResourceConfig config = new ResourceConfig();
        config.register(BloggingClass.class);
        config.packages("com.oracle.faas.jrestlessexample");

        init(config);

        start();
    }

}
