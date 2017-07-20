package com.oracle.faas.jrestlessexample;


import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Path("/route")
public class BloggingClass {
    private List<BlogPost> postList = new ArrayList<>();

    @Path("/pong")
    @GET
    public String ping(){ return "pong"; }

    @GET
    @Path("/{title}")
    @Produces({MediaType.APPLICATION_JSON})
    public String getPost(@PathParam("title") String title) throws Exception {
        BlogPost post = getPostByTitle(title);
        ObjectMapper map = new ObjectMapper();

        return map.writeValueAsString(post);
    }

    @GET
    @Path("/text")
    @Produces({MediaType.TEXT_PLAIN})
    public String tryType(){
        return "Hello";
    }

    @POST
    @Path("/reverse")
    public String reverse(String str){
        return "Expects an InputStream";
    }

    @POST
    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public String createPost(BlogPost post) {
        postList.add(post);
        return post.getTitle();
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
}
