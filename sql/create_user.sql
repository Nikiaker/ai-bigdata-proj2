CREATE USER 'streamuser'@'localhost' IDENTIFIED BY 'stream';
CREATE DATABASE IF NOT EXISTS streamdb CHARACTER SET utf8;
GRANT ALL ON streamdb.* TO 'streamuser'@'%';