package com.oracle.faas.jrestlessexample;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.faas.api.FnConfiguration;
import com.oracle.faas.api.InputEvent;
import com.oracle.faas.api.RuntimeContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/route")
public class BloggingClass {
//    @Context
//    InputEvent inputEvent;

//    private FunctionsDatabase database = setUpDatabase(inputEvent);
    private List<BlogPost> postList = new ArrayList<>();

    public BloggingClass() throws Exception {
    }


    @Path("/context")
    @GET
    public String ping(@Context InputEvent inputEvent){
//        System.err.println(inputEvent.getConfiguration().get("DB_URL"));
        return inputEvent.toString();
    }

    @GET
    @Path("/{title}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getPost(@PathParam("title") String title) throws Exception {
        BlogPost post = getPostByTitle(title);
        ObjectMapper map = new ObjectMapper();

        return map.writeValueAsString(post);
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String createPost(BlogPost post) {
        postList.add(post);
        return(post.getTitle()) + " added";
    }

    @FnConfiguration
    private void setConnection(RuntimeContext ctx) throws Exception{

    }

    private BlogPost getPostByTitle(String title) {
        BlogPost notFound = new BlogPost("Title Not Found");

        for (BlogPost post : postList) {
            if (post.getTitle().equals(title)) {
                return post;
            }
        }
        return notFound;
    }

//    public FunctionsDatabase setUpDatabase(InputEvent inputEvent) throws Exception {
//        Class.forName("com.mysql.cj.jdbc.Driver");
//
//        Connection connection = DriverManager.getConnection(inputEvent.getConfiguration().get("DB_URL"),
//                inputEvent.getConfiguration().get("DB_USER"), inputEvent.getConfiguration().get("DB_PASSWORD"));
//
//        return database = new FunctionsDatabase(connection);
//    }
}
