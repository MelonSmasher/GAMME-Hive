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
CREATE TABLE gamme_hive.drones
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255),
    connection_id INT
);
CREATE UNIQUE INDEX drones_name_uindex ON gamme_hive.drones (name);
CREATE TABLE gamme_hive.jobs
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    email_id INT NOT NULL,
    drone_id INT NOT NULL,
    CONSTRAINT jobs_emails_id_fk FOREIGN KEY (email_id) REFERENCES emails (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT jobs_drones_id_fk FOREIGN KEY (drone_id) REFERENCES drones (id) ON DELETE CASCADE ON UPDATE CASCADE
);