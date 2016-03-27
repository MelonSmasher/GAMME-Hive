CREATE TABLE gamme_hive.emails
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    progress INT DEFAULT 0,
    processing BOOL DEFAULT FALSE ,
    processed BOOL DEFAULT FALSE ,
    queue BOOL DEFAULT TRUE
);
CREATE UNIQUE INDEX emails_email_uindex ON gamme_hive.emails (email);