CREATE TABLE gamme_hive.emails
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    progress INT DEFAULT 0,
    processing BOOL DEFAULT FALSE ,
    processed BOOL DEFAULT FALSE ,
    queue BOOL DEFAULT TRUE
);
CREATE TABLE gamme_hive.servers
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    address VARCHAR(255) NOT NULL,
    processed_email BOOL DEFAULT FALSE  NOT NULL,
    queued_email BOOL DEFAULT TRUE  NOT NULL
);
CREATE UNIQUE INDEX servers_address_uindex ON gamme_hive.servers (address);
CREATE UNIQUE INDEX emails_email_uindex ON gamme_hive.emails (email);
CREATE TABLE gamme_hive.drones
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    connection_id INT NOT NULL
);
CREATE UNIQUE INDEX drones_name_uindex ON gamme_hive.drones (name);
CREATE TABLE gamme_hive.jobs
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    drone_id INT NOT NULL,
    job_name VARCHAR(255) NOT NULL,
    CONSTRAINT jobs_drones_id_fk FOREIGN KEY (drone_id) REFERENCES drones (id)
);
CREATE UNIQUE INDEX jobs_job_name_uindex ON gamme_hive.jobs (job_name);
CREATE TABLE gamme_hive.job_emails
(
    id INT PRIMARY KEY AUTO_INCREMENT,
    job_id INT NOT NULL,
    email_id INT NOT NULL,
    CONSTRAINT job_emails_jobs_id_fk FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT job_emails_emails_id_fk FOREIGN KEY (email_id) REFERENCES emails (id)
);