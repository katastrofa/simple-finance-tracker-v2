CREATE DATABASE sftv2 CHARACTER SET 'utf8';
CREATE USER 'sftv2_usr'@'localhost' IDENTIFIED BY 'asdf1234';
GRANT ALL ON sftv2.* TO 'sftv2_usr'@'localhost';
