package com.healthcare.tracking.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "patient_vitals_telemetry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientVitalsIndexDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String patientId;

    @Field(type = FieldType.Keyword)
    private String deviceId;

    @Field(type = FieldType.Integer)
    private int heartRateBpm;

    @Field(type = FieldType.Integer)
    private int systolicBp;

    @Field(type = FieldType.Integer)
    private int diastolicBp;

    @Field(type = FieldType.Double)
    private double oxygenSaturationSpO2;

    @Field(type = FieldType.Double)
    private double bloodGlucoseMgDl;

    @Field(type = FieldType.Keyword)
    private String anomalyFlag; // NORMAL, TACHYCARDIA, HYPOXIA, HYPERTENSIVE_CRISIS, HYPOGLYCEMIA

    @Field(type = FieldType.Date)
    private Instant recordedAt;
}
