package com.oracle.faas.jrestlessexample.blog;

import com.oracle.faas.api.RuntimeContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;

@Path("/route")
public class BloggingResource {
    private BlogStore database;

    public BloggingResource(@Context RuntimeContext context){
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

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String createPost(BlogPost post) {
        database.postData(post);
        return (post.getTitle()) + " added";
    }

    public BlogStore setUpDatabase(RuntimeContext context) {
        try {
            Class.forName(context.getConfigurationByKey("DB_DRIVER").orElseThrow(() -> new RuntimeException("DB_DRIVER not configurable")));

            Connection connection = null;

            connection = DriverManager.getConnection(context.getConfigurationByKey("DB_URL").orElseThrow(() -> new RuntimeException("DB_URL not configurable")),
                    context.getConfigurationByKey("DB_USER").orElseThrow(() -> new RuntimeException("DB_USER not configurable")),
                    context.getConfigurationByKey("DB_PASSWORD").orElseThrow(() -> new RuntimeException("DB_PASSWORD not configurable")));
            return new BlogStore(connection);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
