package com.example.fnjrestless.blog;


import com.jrestless.fnproject.FnFeature;
import com.jrestless.fnproject.FnRequestHandler;
import org.glassfish.jersey.server.ResourceConfig;

public class BloggingApp extends FnRequestHandler {

    public BloggingApp() {

        ResourceConfig config = new ResourceConfig();
        config.packages(getClass().getPackage().getName());
        config.register(FnFeature.class);

        init(config);

        start();
    }
}
