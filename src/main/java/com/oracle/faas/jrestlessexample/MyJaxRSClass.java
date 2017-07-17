package com.oracle.faas.jrestlessexample;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class MyJaxRSClass {


    public MyJaxRSClass(){
        System.err.println("Constructing...");
    }


    @GET
    public String ping(){ return "pong"; }

}
