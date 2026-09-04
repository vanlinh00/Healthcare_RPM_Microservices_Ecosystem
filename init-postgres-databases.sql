-- PostgreSQL Multi-Database Initialization for Healthcare Microservices Ecosystem
CREATE DATABASE keycloak_db;
CREATE DATABASE user_auth_db;
CREATE DATABASE appointment_db;
CREATE DATABASE care_dispatch_db;
CREATE DATABASE fulfillment_db;
CREATE DATABASE tracking_db;
CREATE DATABASE notification_db;
CREATE DATABASE healthcare_chat_db;

GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE user_auth_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE appointment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE care_dispatch_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE fulfillment_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE tracking_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE healthcare_chat_db TO postgres;
