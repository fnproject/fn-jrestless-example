package com.example.fnjrestless.blog;

import com.fnproject.fn.api.RuntimeContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Driver;
import java.util.List;
import java.util.Properties;

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

    private BlogStore setUpDatabase(RuntimeContext context) {
        try {
            Class<Driver> driver = (Class<Driver>) Class.forName(context.getConfigurationByKey("DB_DRIVER").orElseThrow(() -> new RuntimeException("DB_DRIVER not configured")));

            String dbUrl = context.getConfigurationByKey("DB_URL").orElseThrow(() -> new RuntimeException("DB_URL not configured"));
            String dbUser = context.getConfigurationByKey("DB_USER").orElseThrow(() -> new RuntimeException("DB_USER not configured"));
            String dbPasswd = context.getConfigurationByKey("DB_PASSWORD").orElseThrow(() -> new RuntimeException("DB_PASSWORD not configured"));


            Properties info = new Properties();
            info.setProperty("user",dbUser);
            info.setProperty("password",dbPasswd);

            Connection connection = driver.getDeclaredConstructor().newInstance().connect(dbUrl,info);

            return new BlogStore(connection);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
