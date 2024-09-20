INSERT INTO role(name) VALUES ('USER');
INSERT INTO role(name) VALUES ('MODERATOR');
INSERT INTO role(name) VALUES ('ADMIN');

INSERT INTO user_lab (id, email, username, password) VALUES (1, 'josep@tecnocampus.cat', 'josep', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');
INSERT INTO user_lab (id, email, username, password) VALUES (2, 'jordi@tecnocampus.cat', 'jordi', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');
INSERT INTO user_lab (id, email, username, password) VALUES (3, 'maria@tecnocampus.cat', 'maria', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');
INSERT INTO user_lab (id, email, username, password) VALUES (4, 'admin@tecnocampus.cat', 'admin', '$2a$10$fVKfcc47q6lrNbeXangjYeY000dmjdjkdBxEOilqhapuTO5ZH0co2');


INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (1, 1);
INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (2, 3);
INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (3, 1);
INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (3, 3);
INSERT INTO user_roles (USER_ID, ROLE_ID) VALUES (4, 3);


