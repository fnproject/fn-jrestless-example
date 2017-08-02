CREATE SCHEMA IF NOT EXISTS test;
DROP TABLE IF EXISTS Blogpost;
CREATE TABLE Blogpost(title VARCHAR(512), date VARCHAR(255), author VARCHAR(255), body VARCHAR(4096));
INSERT INTO Blogpost (title, date, author, body) VALUES ('Testing', 'Friday', 'Rae', 'Data to retrieve');