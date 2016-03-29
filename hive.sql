CREATE TABLE drones
(
  id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL,
  connection_id INT(11) NOT NULL
);
CREATE UNIQUE INDEX drones_name_uindex ON drones (name);
CREATE TABLE emails
(
  id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  progress INT(11) DEFAULT '0',
  processing TINYINT(1) DEFAULT '0',
  processed TINYINT(1) DEFAULT '0',
  queue TINYINT(1) DEFAULT '1',
  pass INT(11) DEFAULT '0' NOT NULL
);
CREATE UNIQUE INDEX emails_email_uindex ON emails (email);
CREATE TABLE jobs
(
  id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
  drone_id INT(11) NOT NULL,
  job_name VARCHAR(255) NOT NULL,
  completed TINYINT(1) DEFAULT '0',
  progress INT(11) DEFAULT '0'
);
CREATE INDEX jobs_drones_id_fk ON jobs (drone_id);
CREATE UNIQUE INDEX jobs_job_name_uindex ON jobs (job_name);
CREATE TABLE servers
(
  id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
  address VARCHAR(255) NOT NULL,
  current_jobs INT(11) DEFAULT '0'
);
CREATE UNIQUE INDEX servers_address_uindex ON servers (address);
CREATE TABLE job_emails
(
  id INT(11) PRIMARY KEY NOT NULL AUTO_INCREMENT,
  job_id INT(11) NOT NULL,
  email_id INT(11) NOT NULL
);
CREATE INDEX job_emails_emails_id_fk ON job_emails (email_id);
CREATE INDEX job_emails_jobs_id_fk ON job_emails (job_id);