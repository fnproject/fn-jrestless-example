CREATE SCHEMA IF NOT EXISTS POSTS;
DROP TABLE IF EXISTS Blogpost;
CREATE TABLE Blogpost(title VARCHAR(512), dt VARCHAR(255), author VARCHAR(255), body VARCHAR(4096));
INSERT INTO Blogpost (title, dt, author, body) VALUES ('Testing', 'Friday', 'Rae', 'Welcome to Fn! we have just created a serverless Jersey App');
