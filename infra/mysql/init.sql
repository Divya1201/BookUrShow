-- Database-per-service: each microservice owns its schema and never reads another service's tables.
CREATE DATABASE IF NOT EXISTS userdb;
CREATE DATABASE IF NOT EXISTS catalogdb;
CREATE DATABASE IF NOT EXISTS bookingdb;
CREATE DATABASE IF NOT EXISTS paymentdb;

CREATE USER IF NOT EXISTS 'ticket'@'%' IDENTIFIED BY 'ticket123';
GRANT ALL PRIVILEGES ON userdb.*    TO 'ticket'@'%';
GRANT ALL PRIVILEGES ON catalogdb.* TO 'ticket'@'%';
GRANT ALL PRIVILEGES ON bookingdb.* TO 'ticket'@'%';
GRANT ALL PRIVILEGES ON paymentdb.* TO 'ticket'@'%';
FLUSH PRIVILEGES;
