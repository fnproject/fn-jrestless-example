package com.oracle.faas.jrestlessexample;


import com.oracle.faas.api.RuntimeContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

@Path("/route")
public class BloggingClass {
    private FunctionsDatabase database;

    public BloggingClass(@Context RuntimeContext context){
        database = setUpDatabase(context);
    }

    @GET
    @Path("/blogs")
    @Produces({MediaType.APPLICATION_JSON})
    public List<BlogPost> getAllPosts() {
        return database.getAllPosts();
    }

    @GET
    @Path("/html")
    @Produces({MediaType.TEXT_HTML})
    public InputStream getWebPage() {
        return this.getClass().getResourceAsStream("/index.html");
    }

    @GET
    @Path("/")
    @Produces({MediaType.APPLICATION_JSON})
    public BlogPost getPost(@QueryParam("title") String title) {
        BlogPost post = database.getData(title);
        return post;
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String createPost(BlogPost post) {
        database.postData(post);
        return (post.getTitle()) + " added";
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
