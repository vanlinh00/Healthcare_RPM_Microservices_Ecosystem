export interface MicroserviceInfo {
  id: string;
  name: string;
  tech: string;
  port: number;
  status: 'UP' | 'DOWN' | 'DEGRADED';
  instances: number;
  health: string;
  role: string;
  traffic: string;
}

export interface SagaStepResult {
  stepNumber: number;
  name: string;
  service: string;
  status: 'SUCCESS' | 'FAILED' | 'COMPENSATED' | 'PENDING';
  detail: string;
  durationMs: number;
}

export interface SagaExecutionResponse {
  sagaId: string;
  appointmentId: string;
  success: boolean;
  finalStatus: string;
  copay?: string;
  steps: SagaStepResult[];
  outboxId?: string;
  failureReason?: string;
}

export interface OutboxEventItem {
  id: string;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  payload: any;
  status: 'PENDING' | 'SENT' | 'FAILED';
  retryCount: number;
  createdAt: string;
  processedAt?: string;
}

export interface KafkaTopicMessage {
  id: string;
  key: string;
  payload: any;
  timestamp: string;
  partition: number;
}

export interface PatientVitalTelemetry {
  id?: string;
  patientId: string;
  deviceId: string;
  heartRate: number;
  systolicBp: number;
  diastolicBp: number;
  spo2: number;
  bloodGlucose: number;
  anomalyFlag: string;
  isEmergency?: boolean;
  recordedAt?: string;
}

export interface MedicalRecordHit {
  id: string;
  patientId: string;
  patientName: string;
  doctorId: string;
  doctorName: string;
  diagnosis: string;
  symptoms: string[];
  icd10Codes: string[];
  clinicalNotes: string;
  prescribedMedications: string;
  consultationDate: string;
  anomalyRisk: 'LOW' | 'MODERATE' | 'HIGH' | 'CRITICAL';
}

export interface CodebaseFileItem {
  path: string;
  category: string;
  description: string;
}
