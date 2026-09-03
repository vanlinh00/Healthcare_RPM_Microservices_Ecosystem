# Healthcare & Remote Patient Monitoring (RPM) Microservices Ecosystem

Enterprise-grade, distributed, HIPAA-compliant microservices platform designed for real-time healthcare monitoring, clinical workflow automation, multi-role IAM, distributed appointment scheduling, and IoT emergency telemetry.

Built with **Java 17**, **Spring Boot 3.4**, **Spring Cloud (2024.0)**, **Keycloak 24**, **PostgreSQL 16**, **Redisson / Redis 7.2**, **Apache Kafka (KRaft)**, and **Elasticsearch 8.17**.

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
┌───────▼────────┐    ┌───────▼────────┐              ┌───────▼────────┐
│   tracking-    │    │ notification-  │              │ Service        │
│   service      │    │ service        │              │ Registry       │
│   (Port 8085)  │    │ (Port 8086)    │              │ (Eureka 8761)  │
└────────────────┘    └────────────────┘              └────────────────┘
```

---

## 📦 Microservices Directory & Responsibilities

| Service Name | Port | Primary Responsibilities & Technologies |
| :--- | :--- | :--- |
| **`kong-gateway`** | `8000 / 8001` | Cloud-Native Kong API Gateway (DB-less declarative mode), CORS, Rate Limiting (300 req/min), Correlation ID, Prometheus metrics. |
| **`service-registry`** | `8761` | Netflix Eureka Discovery Server with dual-zone heartbeat tracking and instance failover. |
| **`user-auth-service`** | `8081` | Keycloak 24 OIDC integration, Google Social Login (IdP Broker federation), Direct Access Grant, 6 RBAC roles, TOTP 2FA, HIPAA audit logging. |
| **`appointment-order-service`** | `8082` | Saga State Machine (Orchestrator), Redisson distributed lock (`RLock`), Transactional Outbox, Strategy copay pricing. |
| **`care-dispatch-service`** | `8083` | Weighted responder scoring algorithm (Proximity 40%, Specialty 30%, Workload 20%, Rating 10%). |
| **`fulfillment-service`** | `8084` | Digital Proof of Delivery (POD) with HMAC-SHA256 signatures, cold-chain temperature anomaly breach tracking. |
| **`tracking-service`** | `8085` | Real-time IoT vitals ingestion (ECG, HR, SpO2, BP, Glucose), WebSocket streaming, Elasticsearch 8.17 indexing. |
| **`notification-service`** | `8086` | Event-driven Kafka consumer, Strategy Factory pattern (Email, SMS, Push, Zalo ZNS), Code Blue priority executor. |

---

## 🔑 User & IAM Service (`user-auth-service`) APIs

The User & IAM microservice manages authentication, authorization, Keycloak session management, Google Social Login (IdP federation), and HIPAA compliance auditing.

### 1. User Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Description**: Authenticates users using Keycloak OpenID Connect Direct Access Grants (Resource Owner Password Credentials) and enforces TOTP 2FA.
- **Request Body**:
```json
{
  "usernameOrEmail": "doctor_emily",
  "password": "Password123!",
  "totpCode": "849201",
  "deviceId": "workstation-clinician-04"
}
```
- **Response (`200 OK`)**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "rt-8f92a3c7e01b4d...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_expires_in": 18000,
  "session_state": "sess-4e782a10",
  "totp_required": false,
  "totp_verified": true,
  "user": {
    "id": "usr-doc-204",
    "email": "emily.vance@healthcare.org",
    "username": "doctor_emily",
    "firstName": "Emily",
    "lastName": "Vance, MD",
    "primaryRole": "DOCTOR",
    "roles": ["DOCTOR", "default-roles-healthcare"],
    "totpEnabled": true,
    "active": true
  }
}
```

### 2. User Logout & Session Revocation
- **Endpoint**: `POST /api/v1/auth/logout`
- **Headers**: `Authorization: Bearer <access_token>`
- **Description**: Executes Keycloak Single Sign-Out by invalidating the user's refresh token and terminating the active session.
- **Request Body**:
```json
{
  "refresh_token": "rt-8f92a3c7e01b4d...",
  "all_sessions": false
}
```
- **Response (`200 OK`)**:
```json
{
  "status": "LOGGED_OUT",
  "message": "Successfully logged out from Keycloak IAM. Session and refresh tokens revoked.",
  "revokedAt": "2026-08-27T09:15:30Z",
  "keycloakSessionRevoked": true
}
```

### 3. Refresh Access Token
- **Endpoint**: `POST /api/v1/auth/refresh`
- **Request Body**:
```json
{
  "refresh_token": "rt-8f92a3c7e01b4d..."
}
```
- **Response (`200 OK`)**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "rt-new-token-9b3...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "session_state": "sess-4e782a10"
}
```

### 4. HIPAA Audit Logs
- **Endpoint**: `GET /api/v1/auth/audit-logs`
- **Headers**: `Authorization: Bearer <access_token>` *(Requires `ADMIN` role)*
- **Description**: Retrieves recent login, logout, and 2FA challenge events logged for HIPAA security monitoring.

### 5. Google Social Login - Authorization URL
- **Endpoint**: `GET /api/v1/auth/google/url`
- **Query Parameter**: `redirect_uri` *(Optional, default: `http://localhost:3000/auth/callback`)*
- **Description**: Generates the Keycloak OpenID Connect authorization URL configured with `kc_idp_hint=google`. When opened by clients (web/mobile browsers or popups), Keycloak immediately redirects to Google OAuth 2.0 consent, bypassing the standard Keycloak login screen.
- **Response (`200 OK`)**:
```json
{
  "auth_url": "http://localhost:8080/realms/healthcare-realm/protocol/openid-connect/auth?client_id=healthcare-api-gateway&response_type=code&scope=openid%20profile%20email%20roles&redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Fauth%2Fcallback&kc_idp_hint=google",
  "keycloak_broker_endpoint": "http://localhost:8080/realms/healthcare-realm/broker/google/endpoint",
  "client_id": "healthcare-api-gateway",
  "realm": "healthcare-realm",
  "provider": "google",
  "redirect_uri": "http://localhost:3000/auth/callback"
}
```

### 6. Google Social Login - Authorization Code Exchange
- **Endpoint**: `POST /api/v1/auth/google/callback`
- **Description**: Exchanges the authorization code emitted by Keycloak after Google federated authentication for signed RS256 JWT access and refresh tokens. Syncs user profile in Keycloak and PostgreSQL, assigns roles, and records a HIPAA audit log.
- **Request Body**:
```json
{
  "code": "91a18274-c089-4cb3-911e-08991be249a1.d8e01",
  "redirect_uri": "http://localhost:3000/auth/callback",
  "deviceId": "workstation-icu-01"
}
```
- **Response (`200 OK`)**:
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "rt-google-7cb2a62cc46b3b01578f9140811c94574a45417ffceb256f",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_expires_in": 18000,
  "session_state": "f17bf139-410b-4909-9446-b3788b7e0ac9",
  "scope": "openid email profile healthcare-api roles",
  "totp_required": false,
  "totp_verified": true,
  "user": {
    "id": "usr-google-76c3c70c",
    "email": "doctor.user@healthcare.org",
    "username": "doctor.user@healthcare.org",
    "firstName": "Google",
    "lastName": "User",
    "primaryRole": "PATIENT",
    "roles": ["PATIENT", "default-roles-healthcare"],
    "totpEnabled": false,
    "active": true
  }
}
```

### 7. Google ID Token / Access Token Exchange (RFC 8693)
- **Endpoint**: `POST /api/v1/auth/google/token`
- **Description**: Authenticates using a Google signed ID token (e.g., from Google Sign-In SDK or One Tap) via Keycloak RFC 8693 Token Exchange or federated verification, automatically provisioning the user in Keycloak and PostgreSQL.
- **Request Body**:
```json
{
  "id_token": "eyJhbGciOiJSUzI1NiIsImtpZCI6IjAwZDY...",
  "deviceId": "dev-browser-client"
}
```

### 8. Keycloak Google Identity Provider Setup & Broker Configuration
- **Endpoint**: `GET /api/v1/auth/google/config`
- **Description**: Returns Keycloak broker callback URIs, registration instructions for Google Cloud Console, and current Identity Provider configuration status.
- **Response (`200 OK`)**:
```json
{
  "configured": true,
  "identity_provider_alias": "google",
  "keycloak_broker_redirect_uri": "http://localhost:8080/realms/healthcare-realm/broker/google/endpoint",
  "client_callback_url": "http://localhost:3000/auth/callback",
  "setup_instructions": [
    "1. Open Google Cloud Console -> APIs & Services -> Credentials",
    "2. Create or select an OAuth 2.0 Client ID (Web Application type)",
    "3. In Authorized redirect URIs, add: http://localhost:8080/realms/healthcare-realm/broker/google/endpoint",
    "4. In Authorized JavaScript origins, add: http://localhost:3000 and http://localhost:8080",
    "5. Open Keycloak Admin Console (http://localhost:8080) -> Realm: healthcare-realm",
    "6. Go to Identity Providers -> Add provider -> Google",
    "7. Paste Client ID & Client Secret from Google, toggle Trust Email = ON, and Save",
    "8. Web and mobile applications can now call GET /api/v1/auth/google/url to launch Google login with Keycloak SSO"
  ],
  "metadata": {
    "realm": "healthcare-realm",
    "authServerUrl": "http://localhost:8080",
    "clientId": "healthcare-api-gateway",
    "syncMode": "IMPORT",
    "trustEmail": true
  }
}
```

---

## 📮 Postman Collection

A complete Postman Collection v2.1.0 is provided in `./postman/healthcare-user-auth-service.postman_collection.json`.

It contains:
- **Authentication & Sessions**: Direct password login (Doctor/Admin), 2FA TOTP login, Token Refresh (OIDC), `/me` profile inspection, Logout / Keycloak session revocation, and **Google Social Login (IdP authorization URL, authorization code exchange, Google ID token login, and broker setup configuration)**.
- **HIPAA 2FA & Verification**: Setup TOTP 2FA Secret & QR Code URI, Verify 6-digit TOTP, HIPAA Audit Logs query, Physician Medical License verification.
- **Keycloak IAM Dynamic RBAC**: Create Realm & Composite Roles, Search & Query Roles, Get Role By Name, Update Role Metadata, Delete Roles.
- **Composite Role Hierarchy**: Add and remove child sub-roles dynamically from parent composite roles.
- **User & Group Role Mappings**: Assign/Revoke direct roles to Keycloak users, Assign/Revoke roles to groups, and Audit complete Effective Permissions.
- **Actuator & Observability**: Health checks and Prometheus metrics endpoints.
- **Automated Token Management**: Post-response test scripts automatically extract and store `access_token` and `refresh_token` into collection variables for subsequent authenticated calls.

---

## ⚙️ Key Architectural Patterns Implemented

1. **Saga Orchestration Pattern (`appointment-order-service`)**:
   - Manages distributed transactions across Services (Order Creation ➔ Provider Reservation ➔ Insurance Billing ➔ Dispatch / Fulfillment).
   - Automatically issues compensation transactions (Rollbacks) upon unexpected step failures.

2. **Distributed Locking with Redisson (`RLock`)**:
   - Acquires distributed lock keys (`lock:appointment:slot:{doctorId}:{time}`) to prevent concurrent double-booking across multi-instance pods.

3. **Transactional Outbox Pattern**:
   - Eliminates dual-write anomalies by writing domain entities and outbox events within a single ACID PostgreSQL transaction, published to Kafka via a scheduled de-queue worker.

4. **Strategy Pattern for Pricing & Dispatch**:
   - `CopayPricingStrategy`: Dynamically calculates patient copays based on consultation type (`Standard`, `Specialist`, `Emergency`).
   - `CareDispatchStrategy`: Computes multi-attribute weighted scores to assign optimal first-responders.

5. **Cold-Chain Sensor Breach Verification**:
   - Tracks medication temperature conditions, validating IoT sensor records and creating HMAC-SHA256 signed Proof of Delivery records.

---

## 📊 Observability & Monitoring System (Prometheus & Grafana)

The platform includes an enterprise-grade observability stack combining **Prometheus 2.53** and **Grafana 11.1** for real-time telemetry, JVM health analysis, clinical anomaly tracking, and HIPAA security audit monitoring.

### 1. Prometheus Architecture & Scrape Configuration (`prometheus.yml`)
Scrapes all microservices, API Gateways, and IAM providers via `15s` polling intervals:

| Scrape Target | Port / Metric Path | Scraped Telemetry & Protocol |
| :--- | :--- | :--- |
| **`prometheus`** | `9090 /metrics` | Prometheus TSDB storage, scrape durations, and compaction metrics |
| **`kong-gateway`** | `8001 /metrics` | HTTP requests/sec, P95/P99 latency, upstream connection pools, status codes |
| **`keycloak-iam`** | `8080 /metrics` | Active user sessions, login successes/failures, token exchanges |
| **`service-registry`** | `8761 /actuator/prometheus` | Eureka registered instances, lease renewal rates, heartbeat sync |
| **`user-auth-service`** | `8081 /actuator/prometheus` | OIDC token latency, TOTP validations, HIPAA security audit events |
| **`appointment-order-service`** | `8082 /actuator/prometheus` | Saga transitions, compensation rollbacks, Redisson `RLock` wait times |
| **`care-dispatch-service`** | `8083 /actuator/prometheus` | Nurse dispatch scoring latency, responder assignment throughput |
| **`fulfillment-service`** | `8084 /actuator/prometheus` | Cold-chain IoT temperatures (`2°C - 8°C`), digital POD signatures |
| **`tracking-service`** | `8085 /actuator/prometheus` | Patient vital signs (HR, SpO2), WebSocket RPM stream counts, ES queries |
| **`notification-service`** | `8086 /actuator/prometheus` | Code-Blue thread pool saturation, Kafka consumer lag, dispatch latency |

### 2. Evaluated Alerting Rules (`alert.rules.yml`)
Configured with 10 critical and warning alerting rules:
- **`MicroserviceInstanceDown`**: Triggers when `up == 0` for >1 minute (Critical).
- **`HighHttp5xxErrorRate`**: Triggers when 5xx errors exceed 5.0% over 5m window (Critical).
- **`HighResponseTimeP95`**: Triggers when P95 HTTP latency exceeds 800ms (Warning).
- **`JvmHeapMemoryExhaustion`**: Triggers when JVM heap exceeds 85% capacity (Warning).
- **`JvmGarbageCollectionStall`**: Triggers when GC pause exceeds 1.5s (Warning).
- **`HikariCpPoolSaturation`**: Triggers when active database connections reach 90% of pool (Critical).
- **`ColdChainTemperatureAnomaly`**: Triggers when medical shipment temp is `< 2°C` or `> 8°C` (Critical).
- **`PatientCriticalVitalAnomaly`**: Triggers Code-Blue notification if patient HR `> 140` or SpO2 `< 90%` (Emergency).
- **`HipaaBruteForceAuthSpike`**: Triggers if failed authentications exceed 10/min (Critical).
- **`KafkaConsumerLagHigh`**: Triggers if consumer group lag exceeds 500 records for >3m (Warning).

### 3. Provisioned Grafana 11 Dashboards (`/grafana/dashboards/`)
Dashboards are automatically mounted and pre-configured via `/grafana/provisioning/`:
1. **`healthcare-microservices-overview.json`**:
   - System Availability gauge (`up`) across all microservices
   - Real-time HTTP Request Rate (RPS) per service
   - P95 / P99 Latency Heatmaps
   - HTTP 2xx / 4xx / 5xx Status Code distribution
2. **`jvm-and-infrastructure.json`**:
   - JVM Heap memory consumption vs max limit
   - Garbage Collector (G1 / ZGC) pause durations
   - Process & System CPU utilization percentages
   - HikariCP Active vs Idle connection pool metrics
   - Kafka Topic ingestion rates & consumer group lag
3. **`clinical-rpm-and-hipaa.json`**:
   - Continuous ICU Bed & RPM Wearable telemetry streams (HR bpm, SpO2 %)
   - Fulfillment Cold-Chain temperature range bounds (2°C - 8°C safe zone)
   - HIPAA Security Audit events (Auth success, Failed logins, TOTP 2FA, Google SSO)
   - Distributed Saga transaction state tracking (Completions vs Rollbacks)

### 4. Kubernetes Deployment Manifest (`k8s/06-monitoring-prometheus-grafana.yaml`)
Includes ConfigMaps for scrape configs and alert rules, Persistent Deployments, and ClusterIP Services ready for Kubernetes cluster deployment:
```bash
kubectl apply -f k8s/06-monitoring-prometheus-grafana.yaml
```

### 5. Telemetry & Simulation API Endpoints
| Endpoint | Method | Description |
| :--- | :--- | :--- |
| **`/actuator/prometheus`** | `GET` | OpenMetrics text format scrape endpoint for Prometheus |
| **`/api/v1/monitoring/overview`** | `GET` | High-level system KPIs, target summaries, and active firing alerts |
| **`/api/v1/monitoring/prometheus/targets`** | `GET` | Active Prometheus scrape targets, scrape intervals, and health status |
| **`/api/v1/monitoring/prometheus/alerts`** | `GET` | Live evaluated alerting rules with status (`inactive`, `firing`) |
| **`/api/v1/monitoring/prometheus/query`** | `GET` | PromQL query execution engine (`?query=up`) |
| **`/api/v1/monitoring/grafana/dashboards`** | `GET` | Catalog of provisioned Grafana dashboards |
| **`/api/v1/monitoring/grafana/dashboards/:uid`** | `GET` | Exportable Grafana dashboard JSON models |
| **`/api/v1/monitoring/simulate`** | `POST` | Fault injection for alerts (`coldchain_breach`, `5xx_spike`, `vital_spike`, `reset`) |

---

## 🚀 Quickstart & Local Setup

### Prerequisites
- **JDK 17** (Eclipse Temurin / OpenJDK 17)
- **Maven 3.9+**
- **Docker & Docker Compose**
- **Node.js 20+** & **npm**

### Step 1: Clone and Start Backing Infrastructure
```bash
# Clone the repository
git clone <repository-url>
cd healthcare-rpm-ecosystem

# Launch PostgreSQL, Redis, Kafka, Keycloak, and Elasticsearch
docker compose up -d
```

### Step 2: Build All Java Microservices
```bash
# Clean compile and package all microservices via Maven root POM
mvn clean package -DskipTests
```

### Step 3: Run the Services
You can run individual Spring Boot applications or start them in sequence:
```bash
# 1. Service Registry
java -jar microservices/service-registry/target/service-registry-1.0.0-SNAPSHOT.jar

# 2. User & Auth Service
java -jar microservices/user-auth-service/target/user-auth-service-1.0.0-SNAPSHOT.jar

# 3. Appointment & Order Service
java -jar microservices/appointment-order-service/target/appointment-order-service-1.0.0-SNAPSHOT.jar

# 4. Kong API Gateway
# Managed via Docker Compose (Port 8000 Proxy / 8001 Admin)
docker compose up -d kong
```

### Step 4: Run the Observability & Interactive Console UI
```bash
npm install
npm run dev
```
Open [http://localhost:3000](http://localhost:3000) in your browser to access the control center.

---

## ☸️ Kubernetes Deployment

Production-ready Kubernetes manifests are provided under the `/k8s` directory:

```bash
# Apply namespace, ConfigMaps, and Secrets
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml

# Deploy Stateful infrastructure
kubectl apply -f k8s/postgres-statefulset.yaml
kubectl apply -f k8s/redis-statefulset.yaml
kubectl apply -f k8s/kafka-statefulset.yaml

# Deploy Microservices, HPA, and Ingress
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
```

---

## 🛡️ Security & HIPAA Compliance

- **Stateless Bearer Authentication**: Keycloak RS256 asymmetric public-key signature verification at the API Gateway.
- **Role-Based Access Control (RBAC)**: Enforces `PATIENT`, `DOCTOR`, `NURSE`, `PHARMACIST`, `LAB_TECH`, and `ADMIN` authority levels.
- **Audit Trails**: Security and clinical operations are recorded in immutable audit tables with client IP, timestamp, and user agents.
- **Zero-Trust Network**: Downstream services validate claims passed by the API Gateway's `JwtAuthenticationRelayFilter`.

---

## 📄 License
This project is licensed under the Apache 2.0 License.
