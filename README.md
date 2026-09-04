# Healthcare & Remote Patient Monitoring (RPM) Microservices Ecosystem

Enterprise-grade, distributed, HIPAA-compliant microservices platform designed for real-time healthcare monitoring, clinical workflow automation, multi-role IAM, distributed appointment scheduling, telehealth chat, and IoT emergency telemetry.

Built with **Java 17**, **Spring Boot 3.4**, **Spring Cloud (2024.0)**, **Keycloak 24**, **PostgreSQL 16**, **Redisson / Redis 7.2**, **Apache Kafka (KRaft)**, **Elasticsearch 8.17**, **Kong API Gateway**, **Prometheus**, and **Grafana 11**.

---

## 🎓 Complete Skills & Competencies Roadmap (What You Will Learn)

This project is engineered as an end-to-end masterclass in modern enterprise software engineering. By studying, running, and extending this codebase, you will gain hands-on proficiency in the following core disciplines:

```
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   SKILLS & COMPETENCY MATRIX                                           │
├──────────────────────────┬──────────────────────────┬──────────────────────────┬───────────────────────┤
│ 1. Distributed Systems   │ 2. Security & IAM        │ 3. Real-Time & IoT       │ 4. DevOps & SRE       │
│ • Saga Orchestrator      │ • Keycloak 24 OIDC       │ • WebSockets & STOMP     │ • Docker Compose      │
│ • Transactional Outbox   │ • OAuth 2.0 & RFC 8693   │ • Redis Pub/Sub          │ • Kubernetes (K8s)    │
│ • Distributed Locks      │ • Google IdP Broker      │ • IoT Telemetry Stream   │ • Prometheus 2.53     │
│ • Kafka KRaft Messaging  │ • Dynamic RBAC           │ • Elasticsearch 8.17     │ • Grafana 11 As-Code  │
│ • Kong API Gateway       │ • TOTP 2FA (RFC 6238)    │ • Presence & CCU         │ • HikariCP Tuning     │
│ • Eureka Discovery       │ • HIPAA Audit Logging    │ • Cold-Chain Sensor Pod  │ • Alerting Rules      │
└──────────────────────────┴──────────────────────────┴──────────────────────────┴───────────────────────┘
```

---

### 1. Enterprise Java & Backend Engineering
- **Modern Java 17 LTS**: Immutability with Java Records, pattern matching for `switch`, sealed types, advanced Stream API pipelines, and modern concurrency (`CompletableFuture`, virtual-thread-ready architecture).
- **Spring Boot 3.4 & Spring Cloud 2024**:
  - Declarative REST APIs with Spring Web and OpenAPI 3.0 (SpringDoc).
  - Spring Data JPA with PostgreSQL, composite keys, custom native queries, and schema lifecycle control.
  - Connection pool tuning using **HikariCP** (leak detection, connection timeouts, maximum pool size balancing).
  - Production readiness via **Spring Boot Actuator** and **Micrometer** telemetry exporters.
- **Maven Multi-Module Architecture**:
  - Parent POM dependency inheritance and BOM (Bill of Materials) version synchronization.
  - Strict profile isolation for local development, staging, and containerized deployment.

---

### 2. Distributed Systems & Advanced Architectural Patterns
- **Saga Orchestration Pattern (`appointment-order-service`)**:
  - State machine orchestrating multi-service distributed transactions: Order Creation ➔ Slot Reservation ➔ Insurance Billing ➔ Notification.
  - Automatic triggering of **Compensating Transactions (Rollbacks)** if downstream services fail.
- **Transactional Outbox Pattern**:
  - Guaranteed event delivery without dual-write inconsistencies between PostgreSQL and Kafka.
  - Atomic local database commits combined with asynchronous outbox polling worker threads.
- **Distributed Locking with Redisson (`RLock`)**:
  - High-precision Redis distributed locks (`lock:appointment:slot:{doctorId}:{time}`) with lease timeouts to prevent race conditions and concurrent double-booking across multi-instance pods.
- **Event-Driven Architecture (EDA) with Apache Kafka**:
  - Operating Kafka in modern **KRaft mode** (no ZooKeeper dependency).
  - High-throughput partitioning, consumer groups, idempotency, and consumer lag monitoring.
- **API Gateway Pattern (`kong-gateway`)**:
  - Cloud-native, DB-less declarative Kong routing (`kong.yml`).
  - Centralized Rate Limiting (300 req/min), CORS policy enforcement, Correlation ID propagation, and WebSocket upgrade proxying.
- **Service Discovery & Client-Side Balancing (`service-registry`)**:
  - Netflix Eureka cluster registration, instance lease renewals, dual-zone heartbeats, and failover routing.
- **Behavioral Design Patterns**:
  - **Strategy Pattern**: Pluggable copay pricing algorithms (`CopayPricingStrategy`) and multi-channel notification dispatchers.
  - **Weighted Scoring Algorithm**: Clinical dispatch engine scoring responder distance (40%), specialty match (30%), active workload (20%), and rating (10%).

---

### 3. Identity, Access Management (IAM) & HIPAA Compliance
- **Keycloak 24 OIDC & OAuth 2.0**:
  - Direct Access Grants (Resource Owner Password Credentials), Authorization Code flow, and Token Refresh.
  - Public-key asymmetric JWT signature verification (**RS256**) with JWKS endpoints.
  - **RFC 8693 OAuth 2.0 Token Exchange** for cross-service identity delegation.
- **Federated Social Identity (Google Social Login)**:
  - Keycloak Identity Provider (IdP) Broker configuration with `kc_idp_hint=google`.
  - Seamless Google OAuth 2.0 login with automatic user account provisioning and role synchronization.
- **Fine-Grained Dynamic Role-Based Access Control (RBAC)**:
  - 6 clinical roles: `PATIENT`, `DOCTOR`, `NURSE`, `PHARMACIST`, `LAB_TECH`, and `ADMIN`.
  - Composite role hierarchies, dynamic role-mapping management via Keycloak Admin REST APIs.
- **Two-Factor Authentication (2FA/MFA)**:
  - Time-based One-Time Password (TOTP) algorithm compliance (**RFC 6238**).
  - Secure secret generation, QR Code URI generation (`otpauth://`), and verification filters.
- **HIPAA Compliance & Zero-Trust Security**:
  - Immutable audit logs capturing user IDs, timestamps, client IPs, user agents, and security events.
  - Password hashing with PBKDF2/BCrypt and digital signatures (**HMAC-SHA256**) for pharmaceutical Proof of Delivery (POD).

---

### 4. Real-Time Communication, Telehealth & IoT Streaming
- **WebSocket & STOMP Protocol (`chat-service` & `tracking-service`)**:
  - Full-duplex bidirectional communication channels over SockJS and raw WebSockets.
  - Topic subscription routing (`/topic/consultation.{roomId}`, `/topic/vitals.{patientId}`).
- **Redis Pub/Sub & Presence Tracking**:
  - Real-time user presence tracking (online, offline, active CCU) across clustered application nodes.
  - Channel-based pub/sub message synchronization ensuring multi-node scalability.
- **Continuous IoT Medical Telemetry**:
  - High-frequency ingestion of clinical vitals (ECG, Heart Rate, SpO2, Blood Pressure, Blood Glucose).
  - Cold-chain temperature boundary tracking (`2°C - 8°C`) for sensitive biologics and vaccine deliveries.
- **Elasticsearch 8.17 Distributed Search**:
  - Time-series patient vital indexing, high-speed metric aggregation, and HIPAA audit log analytics.

---

### 5. Observability, Site Reliability Engineering (SRE) & Monitoring
- **Prometheus 2.53 Metrics Collection**:
  - Pull-based scraping of all 8 microservices, Kong Gateway, and Keycloak via `/actuator/prometheus`.
  - Custom Micrometer instrumentation: CCU gauges (`chat_active_ccu`), Saga status counters, and HTTP latency histograms.
- **PromQL & Proactive Alert Rules Engine (`alert.rules.yml`)**:
  - Automated evaluation of 11 critical and warning alerts (Instance Down, 5xx Error Rate > 5%, P95 Latency > 800ms, JVM Heap Exhaustion > 85%, HikariCP Saturation > 90%, Cold-Chain Breaches, Code Blue Patient Critical Vitals, HIPAA Brute Force Spikes, and Chat CCU Saturation).
- **Grafana 11 Dashboarding as Code**:
  - Pre-provisioned dashboards for System Overview, JVM & Infrastructure Health, and Clinical RPM / HIPAA Analytics.
- **Fault Injection & Chaos Engineering**:
  - Interactive telemetry simulation triggering real-world alerts to test system resilience.

---

### 6. DevOps, Containerization & Cloud-Native Deployment
- **Docker & Docker Compose**:
  - Multi-stage Docker builds optimizing image layer caching and reducing container attack surface.
  - Complete multi-container orchestration with dependency health checks (`depends_on: condition: service_healthy`).
- **Kubernetes (K8s) Production Manifests**:
  - Modular manifests: Namespaces, ConfigMaps, Secrets, StatefulSets (PostgreSQL, Redis, Kafka), Deployments, ClusterIP Services, Ingress, and Horizontal Pod Autoscaling (HPA).

---

## 🏛 System Architecture & Topology

```
                                 ┌─────────────────────────┐
                                 │   Frontend / Client     │
                                 │   (Web, Mobile, REST)   │
                                 └────────────┬────────────┘
                                              │
                      ┌───────────────────────┴───────────────────────┐
                      │      Kong API Gateway (Ports 8000 / 8001)     │
                      │  (DB-less Declarative Mode, CORS, Rate Limit)  │
                      └───────┬───────────────────────────────┬───────┘
                              │                               │
        ┌─────────────────────┼───────────────────────────────┼─────────────────────┐
        │                     │                               │                     │
┌───────▼────────┐    ┌───────▼────────┐              ┌───────▼────────┐    ┌───────▼────────┐
│  user-auth-    │    │  appointment-  │              │  care-dispatch-│    │  fulfillment-  │
│  service       │    │  order-service │              │  service       │    │  service       │
│  (Port 8081)   │    │  (Port 8082)   │              │  (Port 8083)   │    │  (Port 8084)   │
└───────┬────────┘    └───────┬────────┘              └───────┬────────┘    └───────┬────────┘
        │                     │                               │                     │
        │             ┌───────┴───────────────┐               │                     │
        │             │ Apache Kafka (KRaft)  │◄──────────────┼─────────────────────┘
        │             │ Topics: telemetry,    │               │
        │             │ saga-events, alerts   │               │
        │             └───────┬───────────────┘               │
        │                     │                               │
┌───────▼────────┐    ┌───────▼────────┐              ┌───────▼────────┐    ┌────────────────┐
│   tracking-    │    │ notification-  │              │  chat-service  │    │ Service        │
│   service      │    │ service        │              │  (Port 8087)   │    │ Registry       │
│   (Port 8085)  │    │ (Port 8086)    │              │  (WebSocket)   │    │ (Eureka 8761)  │
└────────────────┘    └────────────────┘              └────────────────┘    └────────────────┘
```

---

## 📦 Microservices Directory & Responsibilities

| Service Name | Port | Primary Responsibilities & Technologies Learned |
| :--- | :--- | :--- |
| **`kong-gateway`** | `8000 / 8001` | Cloud-Native API Gateway, DB-less declarative routing, Rate Limiting (300 req/min), CORS, Correlation ID, WebSocket proxying. |
| **`service-registry`** | `8761` | Netflix Eureka Discovery Server with dual-zone heartbeat tracking and instance failover. |
| **`user-auth-service`** | `8081` | Keycloak 24 OIDC integration, Google Social Login (IdP Broker federation), Direct Access Grant, 6 RBAC roles, TOTP 2FA, HIPAA audit logging. |
| **`appointment-order-service`** | `8082` | Saga State Machine (Orchestrator), Redisson distributed lock (`RLock`), Transactional Outbox, Strategy copay pricing. |
| **`care-dispatch-service`** | `8083` | Weighted responder scoring algorithm (Proximity 40%, Specialty 30%, Workload 20%, Rating 10%). |
| **`fulfillment-service`** | `8084` | Digital Proof of Delivery (POD) with HMAC-SHA256 signatures, cold-chain temperature anomaly breach tracking (`2°C - 8°C`). |
| **`tracking-service`** | `8085` | Real-time IoT vitals ingestion (ECG, HR, SpO2, BP, Glucose), WebSocket streaming, Elasticsearch 8.17 indexing. |
| **`notification-service`** | `8086` | Event-driven Kafka consumer, Strategy Factory pattern (Email, SMS, Push, Zalo ZNS), Code Blue priority executor. |
| **`chat-service`** | `8087` | Realtime Telehealth Consultation chat, WebSocket/STOMP protocol, Redis presence & CCU tracking, PostgreSQL message persistence. |

---

## 🔑 Core API Endpoints

### 1. Authentication & IAM (`user-auth-service`)
- `POST /api/v1/auth/login`: Authenticate with Keycloak Direct Access Grant and optional TOTP 2FA.
- `POST /api/v1/auth/logout`: Revoke active Keycloak sessions and refresh tokens.
- `POST /api/v1/auth/refresh`: Exchange refresh token for a new RS256 JWT access token.
- `GET /api/v1/auth/google/url`: Launch Keycloak federated Google Social Login.
- `POST /api/v1/auth/google/callback`: Exchange Google authorization code for JWT tokens and sync user profile.
- `POST /api/v1/auth/google/token`: RFC 8693 Token Exchange using Google signed ID tokens.
- `GET /api/v1/auth/audit-logs`: Query immutable HIPAA security audit events.

### 2. Clinical Appointments & Distributed Saga (`appointment-order-service`)
- `POST /api/v1/appointments`: Initiate Saga distributed transaction with Redisson `RLock` slot protection.
- `GET /api/v1/appointments/:id`: Retrieve appointment state and Saga execution status.

### 3. Realtime Telehealth Chat (`chat-service`)
- `WS /ws-chat`: STOMP over SockJS endpoint for interactive consultation channels.
- `GET /api/v1/chat/rooms`: List active consultation rooms (Doctor-Patient, Care Team, Emergency Code Blue, Triage).
- `POST /api/v1/chat/rooms/:roomId/messages`: Send messages with clinical role metadata.
- `GET /api/v1/chat/presence`: Inspect active CCU connections and Redis cluster heartbeat status.

### 4. Telemetry & Observability (`Prometheus` & `Grafana`)
- `GET /actuator/prometheus`: OpenMetrics scrape endpoint on every microservice.
- `GET /api/v1/monitoring/overview`: Aggregated system availability, KPIs, and firing alerts.
- `GET /api/v1/monitoring/prometheus/targets`: Scrape targets status, scrape duration, and health.
- `GET /api/v1/monitoring/prometheus/alerts`: Evaluated Prometheus alerting rules (`firing` vs `inactive`).
- `POST /api/v1/monitoring/simulate`: Inject test scenarios (`coldchain_breach`, `vital_spike`, `5xx_spike`, `reset`).

---

## 📮 Postman Collection

A complete Postman Collection v2.1.0 is provided in:
```
./postman/healthcare-user-auth-service.postman_collection.json
```

It includes automated pre-request and post-response scripts to store JWT tokens, test 2FA verification, manage dynamic Keycloak roles, query HIPAA audit logs, and test Google SSO integration.

---

## 🚀 Quickstart & Local Execution

### Prerequisites
- **JDK 17** (Eclipse Temurin / OpenJDK 17)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **Node.js 20+** & **npm**

### Step 1: Start Backing Infrastructure
```bash
# Launch PostgreSQL, Redis, Kafka, Keycloak, Elasticsearch, Kong, Prometheus, and Grafana
docker compose up -d
```

### Step 2: Build All Java Microservices
```bash
# Clean compile and package all microservices via Maven root POM
mvn clean package -DskipTests
```

### Step 3: Run the Microservices
You can run services individually via Spring Boot:
```bash
# Example: Start User & Auth Service
java -jar microservices/user-auth-service/target/user-auth-service-1.0.0-SNAPSHOT.jar

# Example: Start Chat Service
java -jar microservices/chat-service/target/chat-service-1.0.0-SNAPSHOT.jar
```

### Step 4: Run the Interactive Management Dashboard
```bash
npm install
npm run dev
```
Open [http://localhost:3000](http://localhost:3000) to access the interactive web management console, live consultation chat, and observability telemetry center.

---

## ☸️ Kubernetes Deployment

Deploy the entire ecosystem to a Kubernetes cluster using the manifests in `/k8s`:

```bash
# Apply namespace, ConfigMaps, and Secrets
kubectl apply -f k8s/01-namespace-config-secrets.yaml

# Deploy stateful infrastructure (Postgres, Redis, Kafka, Keycloak)
kubectl apply -f k8s/02-backing-services-statefulsets.yaml

# Deploy all microservices
kubectl apply -f k8s/03-microservices-deployments.yaml

# Deploy API Gateway and Ingress
kubectl apply -f k8s/04-kong-gateway-ingress.yaml

# Deploy Prometheus and Grafana Observability
kubectl apply -f k8s/06-monitoring-prometheus-grafana.yaml
```

---

## 🛡️ HIPAA Compliance & Security Highlights

- **Stateless Bearer Authentication**: Keycloak RS256 asymmetric public-key signature verification.
- **Granular RBAC**: Strict role boundaries (`PATIENT`, `DOCTOR`, `NURSE`, `PHARMACIST`, `LAB_TECH`, `ADMIN`).
- **Audit Trails**: Non-repudiable audit records with timestamps, client IPs, and user agents.
- **Zero-Trust Token Propagation**: Gateway-level claim validation and downstream header relay.
- **Cryptographic Integrity**: HMAC-SHA256 digital signatures for critical medication handoffs.

---

## 📄 License
This project is licensed under the Apache 2.0 License.
