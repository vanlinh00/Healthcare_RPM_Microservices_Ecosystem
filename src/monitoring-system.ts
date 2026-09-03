// Monitoring System Engine for Prometheus & Grafana Observability
import fs from 'fs';
import path from 'path';

export interface ScrapeTarget {
  job: string;
  instance: string;
  metricsPath: string;
  health: 'up' | 'down';
  lastScrape: string;
  lastDurationSeconds: number;
  labels: Record<string, string>;
  description: string;
}

export interface PrometheusAlert {
  alert: string;
  group: string;
  state: 'inactive' | 'pending' | 'firing';
  severity: 'warning' | 'critical' | 'emergency';
  summary: string;
  description: string;
  currentValue: string;
  activeSince?: string;
  labels: Record<string, string>;
}

// In-memory telemetry state for the Healthcare RPM platform
class MonitoringEngine {
  private coldChainTemp = 4.2; // Celsius (safe: 2°C - 8°C)
  private coldChainBreaches = 0;
  private patientHeartRate = 78; // bpm (safe: 60 - 100)
  private patientSpo2 = 98.5; // percent (safe: 95% - 100%)
  private patientAnomalies = 0;
  private errorRate5xxPercent = 0.15; // percent
  private jvmHeapUsedBytes = 384 * 1024 * 1024; // 384 MB
  private jvmHeapMaxBytes = 1024 * 1024 * 1024; // 1 GB
  private processCpuUsage = 0.125; // 12.5%
  private systemCpuUsage = 0.185; // 18.5%
  private activePatientStreams = 42;
  private hipaaAuthFailuresLastMinute = 1;
  private downInstances: Set<string> = new Set();
  private requestCounter = 14208;
  private startTime = Date.now();

  constructor() {
    // Slight organic fluctuation every 10s to simulate live telemetry
    setInterval(() => {
      this.requestCounter += Math.floor(Math.random() * 8) + 2;
      if (this.patientHeartRate < 130) {
        this.patientHeartRate = Math.min(95, Math.max(65, +(this.patientHeartRate + (Math.random() * 4 - 2)).toFixed(1)));
      }
      if (this.patientSpo2 > 94) {
        this.patientSpo2 = Math.min(100, Math.max(96, +(this.patientSpo2 + (Math.random() * 0.4 - 0.2)).toFixed(1)));
      }
      if (this.coldChainTemp >= 2 && this.coldChainTemp <= 8) {
        this.coldChainTemp = +(this.coldChainTemp + (Math.random() * 0.2 - 0.1)).toFixed(2);
      }
    }, 8000);
  }

  // Scrape targets registry
  public getScrapeTargets(): ScrapeTarget[] {
    const now = new Date().toISOString();
    const targets: ScrapeTarget[] = [
      {
        job: 'prometheus',
        instance: 'localhost:9090',
        metricsPath: '/metrics',
        health: 'up',
        lastScrape: now,
        lastDurationSeconds: 0.008,
        labels: { tier: 'observability', role: 'metrics-collector' },
        description: 'Prometheus Server self-monitoring'
      },
      {
        job: 'kong-gateway',
        instance: 'kong:8001',
        metricsPath: '/metrics',
        health: 'up',
        lastScrape: now,
        lastDurationSeconds: 0.012,
        labels: { tier: 'api-gateway', proxy: 'cloud-native-kong' },
        description: 'Kong API Gateway Prometheus plugin metrics (RPS, latency, status codes)'
      },
      {
        job: 'keycloak-iam',
        instance: 'keycloak:8080',
        metricsPath: '/metrics',
        health: 'up',
        lastScrape: now,
        lastDurationSeconds: 0.021,
        labels: { tier: 'identity-provider', product: 'keycloak-24' },
        description: 'Keycloak 24 IAM login metrics, user sessions & token exchanges'
      },
      {
        job: 'service-registry',
        instance: 'service-registry:8761',
        metricsPath: '/actuator/prometheus',
        health: 'up',
        lastScrape: now,
        lastDurationSeconds: 0.015,
        labels: { tier: 'microservice', framework: 'spring-cloud-netflix-eureka' },
        description: 'Netflix Eureka Discovery Server instance registry metrics'
      },
      {
        job: 'user-auth-service',
        instance: 'user-auth-service:8081',
        metricsPath: '/actuator/prometheus',
        health: this.downInstances.has('user-auth-service') ? 'down' : 'up',
        lastScrape: now,
        lastDurationSeconds: 0.018,
        labels: { tier: 'microservice', service: 'user-auth', port: '8081' },
        description: 'User Authentication, Keycloak OIDC, TOTP 2FA & HIPAA Security Audit'
      },
      {
        job: 'appointment-order-service',
        instance: 'appointment-order-service:8082',
        metricsPath: '/actuator/prometheus',
        health: this.downInstances.has('appointment-order-service') ? 'down' : 'up',
        lastScrape: now,
        lastDurationSeconds: 0.024,
        labels: { tier: 'microservice', service: 'appointment-order', port: '8082' },
        description: 'Saga Orchestrator, Redisson RLock locks & transactional outbox'
      },
      {
        job: 'care-dispatch-service',
        instance: 'care-dispatch-service:8083',
        metricsPath: '/actuator/prometheus',
        health: this.downInstances.has('care-dispatch-service') ? 'down' : 'up',
        lastScrape: now,
        lastDurationSeconds: 0.019,
        labels: { tier: 'microservice', service: 'care-dispatch', port: '8083' },
        description: 'Weighted nurse dispatch scoring & emergency care routing'
      },
      {
        job: 'fulfillment-service',
        instance: 'fulfillment-service:8084',
        metricsPath: '/actuator/prometheus',
        health: this.downInstances.has('fulfillment-service') ? 'down' : 'up',
        lastScrape: now,
        lastDurationSeconds: 0.016,
        labels: { tier: 'microservice', service: 'fulfillment', port: '8084' },
        description: 'Digital Proof of Delivery HMAC-SHA256 & Cold-Chain IoT sensors'
      },
      {
        job: 'tracking-service',
        instance: 'tracking-service:8085',
        metricsPath: '/actuator/prometheus',
        health: this.downInstances.has('tracking-service') ? 'down' : 'up',
        lastScrape: now,
        lastDurationSeconds: 0.022,
        labels: { tier: 'microservice', service: 'tracking', port: '8085' },
        description: 'Elasticsearch 8.17 Medical search & WebSocket RPM telemetry'
      },
      {
        job: 'notification-service',
        instance: 'notification-service:8086',
        metricsPath: '/actuator/prometheus',
        health: this.downInstances.has('notification-service') ? 'down' : 'up',
        lastScrape: now,
        lastDurationSeconds: 0.014,
        labels: { tier: 'microservice', service: 'notification', port: '8086' },
        description: 'Async Code-Blue emergency notifications & Multi-channel dispatcher'
      }
    ];

    return targets;
  }

  // Evaluated Alert Rules based on alert.rules.yml
  public getAlerts(): PrometheusAlert[] {
    const alerts: PrometheusAlert[] = [];
    const isServiceDown = this.downInstances.size > 0;
    const is5xxHigh = this.errorRate5xxPercent > 5.0;
    const isColdChainBreached = this.coldChainTemp < 2 || this.coldChainTemp > 8;
    const isVitalAnomaly = this.patientHeartRate > 140 || this.patientSpo2 < 90;
    const isHeapCritical = (this.jvmHeapUsedBytes / this.jvmHeapMaxBytes) * 100 > 85;
    const isBruteForce = this.hipaaAuthFailuresLastMinute > 10;

    // 1. Instance Down Alert
    alerts.push({
      alert: 'MicroserviceInstanceDown',
      group: 'healthcare-microservices-alerts',
      state: isServiceDown ? 'firing' : 'inactive',
      severity: 'critical',
      summary: isServiceDown
        ? `Microservice instance ${Array.from(this.downInstances).join(', ')} is DOWN`
        : 'All 8 microservices and gateway instances are operational',
      description: isServiceDown
        ? `Service has stopped responding to Actuator scrapes on /actuator/prometheus.`
        : 'Scrape targets responding with HTTP 200 OK.',
      currentValue: isServiceDown ? '0' : '1',
      labels: { team: 'platform-sre', severity: 'critical' }
    });

    // 2. High HTTP 5xx Error Rate
    alerts.push({
      alert: 'HighHttp5xxErrorRate',
      group: 'healthcare-microservices-alerts',
      state: is5xxHigh ? 'firing' : 'inactive',
      severity: 'critical',
      summary: `High 5xx error rate: ${this.errorRate5xxPercent.toFixed(2)}%`,
      description: 'Threshold is >5.0% over 5m window. Downstream services may be degraded.',
      currentValue: `${this.errorRate5xxPercent.toFixed(2)}%`,
      labels: { team: 'backend-engineering', severity: 'critical' }
    });

    // 3. High Latency P95
    const p95Latency = is5xxHigh ? 1240 : 210;
    alerts.push({
      alert: 'HighResponseTimeP95',
      group: 'healthcare-microservices-alerts',
      state: p95Latency > 800 ? 'firing' : 'inactive',
      severity: 'warning',
      summary: `P95 response latency is ${p95Latency}ms`,
      description: 'Threshold is >800ms. Check slow database queries or network saturation.',
      currentValue: `${p95Latency}ms`,
      labels: { team: 'backend-engineering', severity: 'warning' }
    });

    // 4. JVM Heap Memory Exhaustion
    const heapPct = +((this.jvmHeapUsedBytes / this.jvmHeapMaxBytes) * 100).toFixed(1);
    alerts.push({
      alert: 'JvmHeapMemoryExhaustion',
      group: 'healthcare-microservices-alerts',
      state: isHeapCritical ? 'firing' : 'inactive',
      severity: 'warning',
      summary: `JVM Heap usage is ${heapPct}% (${Math.round(this.jvmHeapUsedBytes / 1024 / 1024)}MB / 1024MB)`,
      description: 'Threshold is >85%. High risk of OutOfMemoryError and GC thrashing.',
      currentValue: `${heapPct}%`,
      labels: { team: 'platform-sre', severity: 'warning' }
    });

    // 5. Cold-Chain Temperature Anomaly
    alerts.push({
      alert: 'ColdChainTemperatureAnomaly',
      group: 'healthcare-clinical-rpm-alerts',
      state: isColdChainBreached ? 'firing' : 'inactive',
      severity: 'critical',
      summary: isColdChainBreached
        ? `⚠️ Cold-Chain temperature breach: ${this.coldChainTemp.toFixed(1)}°C (Safe limit: 2°C - 8°C)`
        : `Cold-Chain temperatures compliant: ${this.coldChainTemp.toFixed(1)}°C`,
      description: isColdChainBreached
        ? 'Medical courier IoT sensor reported thermal threshold violation. Immediate biologic rescue protocol required.'
        : 'All temperature logs within valid GDP/HIPAA compliance boundary.',
      currentValue: `${this.coldChainTemp.toFixed(1)}°C`,
      labels: { team: 'pharmacy-logistics', compliance: 'hipaa-gdp', severity: 'critical' }
    });

    // 6. Patient Critical Vital Sign Anomaly
    alerts.push({
      alert: 'PatientCriticalVitalAnomaly',
      group: 'healthcare-clinical-rpm-alerts',
      state: isVitalAnomaly ? 'firing' : 'inactive',
      severity: 'emergency',
      summary: isVitalAnomaly
        ? `🚨 CODE BLUE: Critical vital sign anomaly on ICU Bed (HR: ${this.patientHeartRate} bpm, SpO2: ${this.patientSpo2}%)`
        : `Patient vitals stable (HR: ${this.patientHeartRate} bpm, SpO2: ${this.patientSpo2}%)`,
      description: isVitalAnomaly
        ? 'Automated RPM anomaly detector dispatched priority emergency alert to nursing staff.'
        : 'Continuous ICU telemetry within clinical baseline bounds.',
      currentValue: `HR: ${this.patientHeartRate} bpm, SpO2: ${this.patientSpo2}%`,
      labels: { team: 'emergency-clinical-care', code: 'code-blue', severity: 'emergency' }
    });

    // 7. HIPAA Security Brute Force
    alerts.push({
      alert: 'HipaaBruteForceAuthSpike',
      group: 'healthcare-clinical-rpm-alerts',
      state: isBruteForce ? 'firing' : 'inactive',
      severity: 'critical',
      summary: isBruteForce
        ? `HIPAA Alert: ${this.hipaaAuthFailuresLastMinute} failed logins in past minute`
        : `Authentication rate normal (${this.hipaaAuthFailuresLastMinute} failed attempt/min)`,
      description: 'Triggered when >10 failed authentication attempts detected in 1 minute window.',
      currentValue: `${this.hipaaAuthFailuresLastMinute} failures/min`,
      labels: { team: 'security-compliance', compliance: 'hipaa-audit', severity: 'critical' }
    });

    return alerts;
  }

  // Export Prometheus Text Format (OpenMetrics scrape endpoint)
  public generatePrometheusMetricsText(): string {
    const uptimeSec = Math.floor((Date.now() - this.startTime) / 1000);
    const heapUsedMb = Math.round(this.jvmHeapUsedBytes / 1024 / 1024);

    return `# HELP up Microservice instance operational status (1 = UP, 0 = DOWN)
# TYPE up gauge
up{job="prometheus",instance="localhost:9090"} 1
up{job="kong-gateway",instance="kong:8001"} 1
up{job="keycloak-iam",instance="keycloak:8080"} 1
up{job="service-registry",instance="service-registry:8761"} 1
up{job="user-auth-service",instance="user-auth-service:8081"} ${this.downInstances.has('user-auth-service') ? 0 : 1}
up{job="appointment-order-service",instance="appointment-order-service:8082"} ${this.downInstances.has('appointment-order-service') ? 0 : 1}
up{job="care-dispatch-service",instance="care-dispatch-service:8083"} ${this.downInstances.has('care-dispatch-service') ? 0 : 1}
up{job="fulfillment-service",instance="fulfillment-service:8084"} ${this.downInstances.has('fulfillment-service') ? 0 : 1}
up{job="tracking-service",instance="tracking-service:8085"} ${this.downInstances.has('tracking-service') ? 0 : 1}
up{job="notification-service",instance="notification-service:8086"} ${this.downInstances.has('notification-service') ? 0 : 1}

# HELP http_server_requests_seconds_count Total HTTP requests served by endpoint
# TYPE http_server_requests_seconds_count counter
http_server_requests_seconds_count{job="user-auth-service",method="POST",status="200",uri="/api/v1/auth/login"} 4120
http_server_requests_seconds_count{job="user-auth-service",method="GET",status="200",uri="/api/v1/auth/google/url"} 894
http_server_requests_seconds_count{job="user-auth-service",method="POST",status="200",uri="/api/v1/auth/google/callback"} 762
http_server_requests_seconds_count{job="user-auth-service",method="POST",status="200",uri="/api/v1/auth/totp/verify"} 1340
http_server_requests_seconds_count{job="user-auth-service",method="GET",status="200",uri="/api/v1/iam/roles"} 520
http_server_requests_seconds_count{job="appointment-order-service",method="POST",status="200",uri="/api/v1/appointments"} 3290
http_server_requests_seconds_count{job="care-dispatch-service",method="POST",status="200",uri="/api/v1/dispatch/score"} 1820
http_server_requests_seconds_count{job="fulfillment-service",method="POST",status="200",uri="/api/v1/fulfillment/pod"} 1240
http_server_requests_seconds_count{job="tracking-service",method="GET",status="200",uri="/api/v1/tracking/rpm"} 9810
http_server_requests_seconds_count{job="notification-service",method="POST",status="200",uri="/api/v1/notifications/send"} 2450
http_server_requests_seconds_count{job="kong-gateway",method="ALL",status="${this.errorRate5xxPercent > 5 ? '503' : '200'}",uri="/api/v1/*"} ${this.requestCounter}

# HELP jvm_memory_used_bytes The amount of used memory in bytes
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",job="user-auth-service"} ${this.jvmHeapUsedBytes}
jvm_memory_used_bytes{area="heap",job="appointment-order-service"} ${Math.round(this.jvmHeapUsedBytes * 0.9)}
jvm_memory_used_bytes{area="heap",job="tracking-service"} ${Math.round(this.jvmHeapUsedBytes * 1.1)}
jvm_memory_used_bytes{area="nonheap",job="user-auth-service"} 83886080

# HELP jvm_memory_max_bytes The maximum amount of memory in bytes that can be used for memory management
# TYPE jvm_memory_max_bytes gauge
jvm_memory_max_bytes{area="heap",job="user-auth-service"} ${this.jvmHeapMaxBytes}
jvm_memory_max_bytes{area="heap",job="appointment-order-service"} ${this.jvmHeapMaxBytes}

# HELP process_cpu_usage The recent CPU usage for the Java Virtual Machine process
# TYPE process_cpu_usage gauge
process_cpu_usage{job="user-auth-service"} ${this.processCpuUsage}
process_cpu_usage{job="appointment-order-service"} ${(this.processCpuUsage * 1.1).toFixed(3)}
process_cpu_usage{job="tracking-service"} ${(this.processCpuUsage * 1.25).toFixed(3)}

# HELP system_cpu_usage The recent CPU usage for the whole operating system
# TYPE system_cpu_usage gauge
system_cpu_usage ${this.systemCpuUsage}

# HELP jvm_threads_live_threads The current number of live threads including both daemon and non-daemon threads
# TYPE jvm_threads_live_threads gauge
jvm_threads_live_threads{job="user-auth-service"} 38
jvm_threads_live_threads{job="appointment-order-service"} 45
jvm_threads_live_threads{job="tracking-service"} 62

# HELP hikaricp_connections_active Active database connections in HikariCP pool
# TYPE hikaricp_connections_active gauge
hikaricp_connections_active{pool="HealthcareHikariCP",job="user-auth-service"} 4
hikaricp_connections_active{pool="HealthcareHikariCP",job="appointment-order-service"} 7
hikaricp_connections_active{pool="HealthcareHikariCP",job="care-dispatch-service"} 3

# HELP hikaricp_connections_idle Idle database connections in HikariCP pool
# TYPE hikaricp_connections_idle gauge
hikaricp_connections_idle{pool="HealthcareHikariCP",job="user-auth-service"} 16
hikaricp_connections_idle{pool="HealthcareHikariCP",job="appointment-order-service"} 13

# HELP hikaricp_connections_max Maximum allowed connections in HikariCP pool
# TYPE hikaricp_connections_max gauge
hikaricp_connections_max{pool="HealthcareHikariCP",job="user-auth-service"} 20
hikaricp_connections_max{pool="HealthcareHikariCP",job="appointment-order-service"} 20

# HELP fulfillment_coldchain_temperature_celsius Real-time sensor temperature in Celsius for medical logistics
# TYPE fulfillment_coldchain_temperature_celsius gauge
fulfillment_coldchain_temperature_celsius{package_id="PKG-8821",sensor_id="COLD-SENS-01",destination="ICU-Central"} ${this.coldChainTemp}
fulfillment_coldchain_temperature_celsius{package_id="PKG-8822",sensor_id="COLD-SENS-02",destination="Pharmacy-East"} 3.9
fulfillment_coldchain_temperature_celsius{package_id="PKG-8823",sensor_id="COLD-SENS-03",destination="Oncology-Wing"} 4.1

# HELP fulfillment_coldchain_breaches_total Total thermal threshold breach events (< 2C or > 8C)
# TYPE fulfillment_coldchain_breaches_total counter
fulfillment_coldchain_breaches_total ${this.coldChainBreaches}

# HELP patient_telemetry_heart_rate_bpm Remote Patient Monitoring real-time heart rate
# TYPE patient_telemetry_heart_rate_bpm gauge
patient_telemetry_heart_rate_bpm{patient_id="PAT-101",bed_id="ICU-BED-04",vital="ECG"} ${this.patientHeartRate}
patient_telemetry_heart_rate_bpm{patient_id="PAT-102",bed_id="ICU-BED-07",vital="ECG"} 74
patient_telemetry_heart_rate_bpm{patient_id="PAT-103",bed_id="RPM-HOME-02",vital="PULSE"} 68

# HELP patient_telemetry_spo2_percent Remote Patient Monitoring blood oxygen saturation percentage
# TYPE patient_telemetry_spo2_percent gauge
patient_telemetry_spo2_percent{patient_id="PAT-101",bed_id="ICU-BED-04"} ${this.patientSpo2}
patient_telemetry_spo2_percent{patient_id="PAT-102",bed_id="ICU-BED-07"} 99.0
patient_telemetry_spo2_percent{patient_id="PAT-103",bed_id="RPM-HOME-02"} 97.8

# HELP patient_telemetry_active_streams Active telemetry sensor feeds connected via WebSockets
# TYPE patient_telemetry_active_streams gauge
patient_telemetry_active_streams ${this.activePatientStreams}

# HELP patient_vitals_anomalies_total Total vital signs anomalous episodes flagged by ML model
# TYPE patient_vitals_anomalies_total counter
patient_vitals_anomalies_total ${this.patientAnomalies}

# HELP hipaa_security_audit_events_total Cumulative HIPAA compliance security audit log events
# TYPE hipaa_security_audit_events_total counter
hipaa_security_audit_events_total{action="LOGIN_SUCCESS",service="user-auth"} 5824
hipaa_security_audit_events_total{action="AUTH_FAILED",service="user-auth"} ${this.hipaaAuthFailuresLastMinute * 14}
hipaa_security_audit_events_total{action="TOTP_VERIFY",service="user-auth"} 1830
hipaa_security_audit_events_total{action="GOOGLE_SSO",service="user-auth"} 940
hipaa_security_audit_events_total{action="ROLE_MUTATION",service="user-auth"} 48

# HELP saga_orchestrator_events_total Distributed Saga transaction state transitions
# TYPE saga_orchestrator_events_total counter
saga_orchestrator_events_total{status="COMPLETED",saga="AppointmentBooking"} 1240
saga_orchestrator_events_total{status="COMPENSATED",saga="AppointmentBooking"} 18
saga_orchestrator_events_total{status="FAILED",saga="AppointmentBooking"} 4

# HELP kafka_producer_record_send_total Kafka messages published by topic
# TYPE kafka_producer_record_send_total counter
kafka_producer_record_send_total{topic="healthcare.appointment.events"} 1840
kafka_producer_record_send_total{topic="healthcare.emergency.events"} 14
kafka_producer_record_send_total{topic="healthcare.fulfillment.coldchain"} 5200

# HELP kafka_consumer_lag_records Kafka consumer group partition lag
# TYPE kafka_consumer_lag_records gauge
kafka_consumer_lag_records{topic="healthcare.appointment.events",consumergroup="appointment-dispatchers"} 2
kafka_consumer_lag_records{topic="healthcare.fulfillment.coldchain",consumergroup="temperature-monitors"} 0
`;
  }

  // Instant vector PromQL simulator
  public executePromQuery(query: string): any {
    const q = (query || '').trim();
    const timestamp = Math.floor(Date.now() / 1000);

    if (q === 'up' || q.startsWith('up{')) {
      const targets = this.getScrapeTargets();
      return {
        status: 'success',
        data: {
          resultType: 'vector',
          result: targets.map(t => ({
            metric: { __name__: 'up', job: t.job, instance: t.instance },
            value: [timestamp, t.health === 'up' ? '1' : '0']
          }))
        }
      };
    }

    if (q.includes('http_server_requests') || q.includes('http_requests_total')) {
      return {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            { metric: { __name__: 'http_server_requests_seconds_count', job: 'user-auth-service', status: '200' }, value: [timestamp, '4120'] },
            { metric: { __name__: 'http_server_requests_seconds_count', job: 'appointment-order-service', status: '200' }, value: [timestamp, '3290'] },
            { metric: { __name__: 'http_server_requests_seconds_count', job: 'tracking-service', status: '200' }, value: [timestamp, '9810'] },
            { metric: { __name__: 'http_server_requests_seconds_count', job: 'kong-gateway', status: this.errorRate5xxPercent > 5 ? '503' : '200' }, value: [timestamp, `${this.requestCounter}`] }
          ]
        }
      };
    }

    if (q.includes('jvm_memory_used_bytes') || q.includes('jvm_heap')) {
      return {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            { metric: { __name__: 'jvm_memory_used_bytes', area: 'heap', job: 'user-auth-service' }, value: [timestamp, `${this.jvmHeapUsedBytes}`] },
            { metric: { __name__: 'jvm_memory_used_bytes', area: 'heap', job: 'appointment-order-service' }, value: [timestamp, `${Math.round(this.jvmHeapUsedBytes * 0.9)}`] },
            { metric: { __name__: 'jvm_memory_used_bytes', area: 'heap', job: 'tracking-service' }, value: [timestamp, `${Math.round(this.jvmHeapUsedBytes * 1.1)}`] }
          ]
        }
      };
    }

    if (q.includes('fulfillment_coldchain_temperature_celsius') || q.includes('coldchain')) {
      return {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            { metric: { __name__: 'fulfillment_coldchain_temperature_celsius', package_id: 'PKG-8821', sensor_id: 'COLD-SENS-01' }, value: [timestamp, `${this.coldChainTemp}`] },
            { metric: { __name__: 'fulfillment_coldchain_temperature_celsius', package_id: 'PKG-8822', sensor_id: 'COLD-SENS-02' }, value: [timestamp, '3.9'] }
          ]
        }
      };
    }

    if (q.includes('patient_telemetry_heart_rate_bpm') || q.includes('heart_rate')) {
      return {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            { metric: { __name__: 'patient_telemetry_heart_rate_bpm', patient_id: 'PAT-101', bed_id: 'ICU-BED-04' }, value: [timestamp, `${this.patientHeartRate}`] },
            { metric: { __name__: 'patient_telemetry_heart_rate_bpm', patient_id: 'PAT-102', bed_id: 'ICU-BED-07' }, value: [timestamp, '74'] }
          ]
        }
      };
    }

    if (q.includes('hipaa_security_audit_events_total') || q.includes('hipaa')) {
      return {
        status: 'success',
        data: {
          resultType: 'vector',
          result: [
            { metric: { __name__: 'hipaa_security_audit_events_total', action: 'LOGIN_SUCCESS' }, value: [timestamp, '5824'] },
            { metric: { __name__: 'hipaa_security_audit_events_total', action: 'AUTH_FAILED' }, value: [timestamp, `${this.hipaaAuthFailuresLastMinute * 14}`] },
            { metric: { __name__: 'hipaa_security_audit_events_total', action: 'GOOGLE_SSO' }, value: [timestamp, '940'] }
          ]
        }
      };
    }

    // Generic fallback for any other query
    return {
      status: 'success',
      data: {
        resultType: 'vector',
        result: [
          {
            metric: { __name__: q || 'metric_result', job: 'healthcare-platform' },
            value: [timestamp, '1.0']
          }
        ]
      }
    };
  }

  // Simulation controls for testing alerts in Prometheus and Grafana
  public simulateScenario(scenario: string): { message: string; updatedState: any } {
    switch (scenario) {
      case 'coldchain_breach':
        this.coldChainTemp = 11.4; // Exceeds 8°C limit
        this.coldChainBreaches += 1;
        return {
          message: 'Cold-chain thermal breach simulated (11.4°C). Alert ColdChainTemperatureAnomaly is now FIRING.',
          updatedState: { coldChainTemp: this.coldChainTemp, breaches: this.coldChainBreaches }
        };

      case '5xx_spike':
        this.errorRate5xxPercent = 8.7; // Exceeds 5% limit
        return {
          message: 'High 5xx server error spike simulated (8.7%). Alert HighHttp5xxErrorRate is now FIRING.',
          updatedState: { errorRate5xxPercent: this.errorRate5xxPercent }
        };

      case 'vital_spike':
        this.patientHeartRate = 162; // Arrhythmia/Tachycardia spike (>140)
        this.patientSpo2 = 88.0; // Desaturation (<90%)
        this.patientAnomalies += 1;
        return {
          message: 'Patient critical vital signs anomaly simulated (HR: 162 bpm, SpO2: 88%). Alert PatientCriticalVitalAnomaly is now FIRING (Code Blue).',
          updatedState: { heartRate: this.patientHeartRate, spo2: this.patientSpo2 }
        };

      case 'heap_exhaustion':
        this.jvmHeapUsedBytes = 940 * 1024 * 1024; // ~91.8% of 1GB
        return {
          message: 'JVM heap exhaustion simulated (91.8%). Alert JvmHeapMemoryExhaustion is now FIRING.',
          updatedState: { heapPercent: 91.8, heapUsedMb: 940 }
        };

      case 'brute_force':
        this.hipaaAuthFailuresLastMinute = 16; // >10 threshold
        return {
          message: 'HIPAA Security brute force attack simulated (16 failed logins/min). Alert HipaaBruteForceAuthSpike is now FIRING.',
          updatedState: { failedLoginsPerMin: 16 }
        };

      case 'service_down':
        this.downInstances.add('appointment-order-service');
        return {
          message: 'Simulated appointment-order-service failure (Instance DOWN). Alert MicroserviceInstanceDown is now FIRING.',
          updatedState: { downInstances: Array.from(this.downInstances) }
        };

      case 'reset':
      default:
        this.coldChainTemp = 4.2;
        this.patientHeartRate = 78;
        this.patientSpo2 = 98.5;
        this.errorRate5xxPercent = 0.15;
        this.jvmHeapUsedBytes = 384 * 1024 * 1024;
        this.hipaaAuthFailuresLastMinute = 1;
        this.downInstances.clear();
        return {
          message: 'All telemetry metrics restored to healthy baseline bounds. All Prometheus alerts returned to INACTIVE.',
          updatedState: {
            coldChainTemp: 4.2,
            patientHeartRate: 78,
            patientSpo2: 98.5,
            errorRate5xxPercent: 0.15,
            downInstances: []
          }
        };
    }
  }

  // System Overview KPIs
  public getOverview(): any {
    const targets = this.getScrapeTargets();
    const alerts = this.getAlerts();
    const upCount = targets.filter(t => t.health === 'up').length;
    const firingAlerts = alerts.filter(a => a.state === 'firing');

    return {
      status: firingAlerts.length === 0 ? 'HEALTHY' : 'ALERTING',
      activeServicesCount: `${upCount}/${targets.length}`,
      kpis: {
        totalRps: (this.requestCounter / Math.max(1, Math.floor((Date.now() - this.startTime) / 1000))).toFixed(1),
        p95LatencyMs: this.errorRate5xxPercent > 5 ? 1240 : 210,
        errorRate5xxPercent: this.errorRate5xxPercent.toFixed(2),
        jvmHeapUsedMb: Math.round(this.jvmHeapUsedBytes / 1024 / 1024),
        jvmHeapMaxMb: Math.round(this.jvmHeapMaxBytes / 1024 / 1024),
        systemCpuPercent: Math.round(this.systemCpuUsage * 100),
        activePatientStreams: this.activePatientStreams,
        patientHeartRateBpm: this.patientHeartRate,
        patientSpo2Percent: this.patientSpo2,
        coldChainTempCelsius: this.coldChainTemp
      },
      targetsSummary: {
        total: targets.length,
        up: upCount,
        down: targets.length - upCount
      },
      alertsSummary: {
        totalRules: alerts.length,
        firingCount: firingAlerts.length,
        firingAlerts: firingAlerts.map(a => ({ alert: a.alert, severity: a.severity, summary: a.summary }))
      },
      grafanaDashboards: [
        {
          uid: 'healthcare-overview',
          title: 'Healthcare & RPM Microservices Overview',
          tags: ['healthcare', 'microservices', 'spring-boot', 'kong'],
          panelsCount: 8,
          refreshRate: '10s'
        },
        {
          uid: 'jvm-infrastructure',
          title: 'JVM & Infrastructure Telemetry',
          tags: ['jvm', 'micrometer', 'hikaricp', 'kafka'],
          panelsCount: 6,
          refreshRate: '10s'
        },
        {
          uid: 'clinical-rpm-hipaa',
          title: 'Clinical RPM Telemetry & HIPAA Security Audit',
          tags: ['clinical', 'rpm', 'telemetry', 'hipaa', 'audit'],
          panelsCount: 8,
          refreshRate: '10s'
        }
      ]
    };
  }
}

export const monitoringEngine = new MonitoringEngine();
