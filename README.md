# Healthcare & Remote Patient Monitoring (RPM) Microservices Ecosystem

Enterprise-grade, distributed, HIPAA-compliant microservices platform designed for real-time healthcare monitoring, clinical workflow automation, multi-role IAM, distributed appointment scheduling, and IoT emergency telemetry.

Built with **Java 17**, **Spring Boot 3.4**, **Spring Cloud (2024.0)**, **Keycloak 24**, **PostgreSQL 16**, **Redisson / Redis 7.2**, **Apache Kafka (KRaft)**, **Elasticsearch 8.17**, and a **React 18 + Vite** interactive observability dashboard.

---

## 🏛 System Architecture & Topology

```
                                 ┌─────────────────────────┐
                                 │  React 18 Observability  │
                                 │    Dashboard (Port 3000)│
                                 └────────────┬────────────┘
                                              │
                      ┌───────────────────────┴───────────────────────┐
                      │    Spring Cloud API Gateway (Port 8080)        │
                      │  (Netty, Route Predicates, JWT Relay Filters)  │
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
| **`service-registry`** | `8761` | Netflix Eureka Discovery Server with dual-zone heartbeat tracking and instance failover. |
| **`api-gateway`** | `8080` | Spring Cloud Gateway (Reactive Netty), `JwtAuthenticationRelayFilter`, CORS headers, rate limiting. |
| **`user-auth-service`** | `8081` | Keycloak 24 OIDC integration, Direct Access Grant, 6 RBAC roles, TOTP 2FA, HIPAA audit logging. |
| **`appointment-order-service`** | `8082` | Saga State Machine (Orchestrator), Redisson distributed lock (`RLock`), Transactional Outbox, Strategy copay pricing. |
| **`care-dispatch-service`** | `8083` | Weighted responder scoring algorithm (Proximity 40%, Specialty 30%, Workload 20%, Rating 10%). |
| **`fulfillment-service`** | `8084` | Digital Proof of Delivery (POD) with HMAC-SHA256 signatures, cold-chain temperature anomaly breach tracking. |
| **`tracking-service`** | `8085` | Real-time IoT vitals ingestion (ECG, HR, SpO2, BP, Glucose), WebSocket streaming, Elasticsearch 8.17 indexing. |
| **`notification-service`** | `8086` | Event-driven Kafka consumer, Strategy Factory pattern (Email, SMS, Push, Zalo ZNS), Code Blue priority executor. |

---

## 🔑 User & IAM Service (`user-auth-service`) APIs

The User & IAM microservice manages authentication, authorization, Keycloak session management, and HIPAA compliance auditing.

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

# 4. API Gateway
java -jar microservices/api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
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
