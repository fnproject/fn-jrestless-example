package com.example.fnjrestless.blog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlogStore {
    final private Connection connection;

    public BlogStore(Connection connection){
        this.connection = Objects.requireNonNull(connection);
    }

    public void postData(BlogPost post)  {
        Objects.requireNonNull(post);
        BlogPost currentPost = getData(post.getTitle());

        if(currentPost.getBody() != null){
            try(PreparedStatement st = connection.prepareStatement("UPDATE Blogpost SET author = ?," +
                    "dt = ?, body = ? WHERE title = ?;")){
                st.setString(1, post.getAuthor());
                st.setString(2, post.getDate());
                st.setString(3, post.getBody());
                st.setString(4, post.getTitle());

                st.executeUpdate();
            } catch(SQLException se){
                se.printStackTrace();
                throw new DataBaseAccessException("error updating database",se);
            }
        }else {
            try(PreparedStatement st = connection.prepareStatement("INSERT INTO Blogpost VALUES (?,?,?,?)")){
                st.setString(1, post.getTitle());
                st.setString(2, post.getDate());
                st.setString(3, post.getAuthor());
                st.setString(4, post.getBody());

                st.executeUpdate();
            } catch(SQLException se){
                se.printStackTrace();
                throw new DataBaseAccessException("error updating database",se);
            }
        }
    }

    public List<BlogPost> getAllPosts() {
        try(PreparedStatement st = connection.prepareStatement("SELECT * FROM Blogpost ORDER BY dt DESC")){
            ResultSet rs = st.executeQuery();

            List<BlogPost> allPosts = new ArrayList<>();
            while(rs.next()){
                String author = rs.getString("author");
                String date = rs.getString("dt");
                String body = rs.getString("body");
                String title = rs.getString("title");

                BlogPost post = new BlogPost();

                post.setAuthor(author);
                post.setBody(body);
                post.setDate(date);
                post.setTitle(title);

                allPosts.add(post);
            }

            return allPosts;

        }catch(SQLException se){

            throw new DataBaseAccessException("error updating database",se);
        }
    }

    public BlogPost getData(String title) {
        Objects.requireNonNull(title);
        try(PreparedStatement st = connection.prepareStatement("SELECT * FROM Blogpost WHERE title = ?")){
            st.setString(1, title);
            ResultSet rs = st.executeQuery();

            BlogPost post = new BlogPost(title);

            while(rs.next()){
                String author = rs.getString("author");
                String date = rs.getString("dt");
                String body = rs.getString("body");

                post.setAuthor(author);
                post.setBody(body);
                post.setDate(date);
            }

            if(post.getBody() == null){
                return new BlogPost("Title Not Found");
            }
            return post;

        }catch(SQLException se){

            throw new DataBaseAccessException("error updating database",se);
        }
    }

    public static class DataBaseAccessException extends RuntimeException {
        public DataBaseAccessException(String msg, Throwable cause){
            super(msg,cause);
            System.err.println("DatabaseAccessException");
        }
    }
}
