INSERT INTO role(name) VALUES ('USER');
INSERT INTO role(name) VALUES ('MODERATOR');
INSERT INTO role(name) VALUES ('ADMIN');

INSERT INTO user_lab (email, username, password) VALUES ('josep@tecnocampus.cat', 'josep', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');
INSERT INTO user_lab (email, username, password) VALUES ('jordi@tecnocampus.cat', 'jordi', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');
INSERT INTO user_lab (email, username, password) VALUES ('maria@tecnocampus.cat', 'maria', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');
INSERT INTO user_lab (email, username, password) VALUES ('admin@tecnocampus.cat', 'admin', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');
INSERT INTO user_lab (email, username, password) VALUES ('moderator@tecnocampus.cat', 'moderator', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');


INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (1, 1);
INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (2, 3);
INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (3, 1);
INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (3, 3);
INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (4, 3);


