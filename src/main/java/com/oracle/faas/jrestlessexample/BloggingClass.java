package com.oracle.faas.jrestlessexample;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.faas.api.RuntimeContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

@Path("/route")
public class BloggingClass {
    private FunctionsDatabase database;

    private List<BlogPost> postList = new ArrayList<>();

    public BloggingClass(@Context RuntimeContext context){
        database = setUpDatabase(context);
    }


//    @Path("/input")
//    @GET
//    public String inputEvent(@Context InputEvent inputEvent){
//        return inputEvent.getConfiguration().get("DB_URL");
//    }
//
//    @Path("/context")
//    @GET
//    public String runtimeContext(@Context RuntimeContext rctx){
//        return rctx.getConfigurationByKey("DB_URL");
//    }

    @GET
    @Path("/{title}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getPost(@PathParam("title") String title) throws Exception {
        BlogPost post = database.getData(title);
        ObjectMapper map = new ObjectMapper();

        return map.writeValueAsString(post);
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String createPost(BlogPost post) {
        database.postData(post);
        return(post.getTitle()) + " added";
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

    public FunctionsDatabase setUpDatabase(RuntimeContext context) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

        Connection connection = null;

            connection = DriverManager.getConnection(context.getConfigurationByKey("DB_URL").orElseThrow(() -> new RuntimeException("DB_URL not configurable")),
                    context.getConfigurationByKey("DB_USER").orElseThrow(() -> new RuntimeException("DB_USER not configurable")),
                    context.getConfigurationByKey("DB_PASSWORD").orElseThrow(() -> new RuntimeException("DB_PASSWORD not configurable")));
            return database = new FunctionsDatabase(connection);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
