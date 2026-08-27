package com.healthcare.tracking.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

@Document(indexName = "medical_records_search")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordIndexDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String patientId;

    @Field(type = FieldType.Keyword)
    private String doctorId;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String diagnosis;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String clinicalNotes;

    @Field(type = FieldType.Keyword)
    private List<String> symptoms;

    @Field(type = FieldType.Keyword)
    private List<String> icd10Codes;

    @Field(type = FieldType.Text)
    private String prescribedMedications;

    @Field(type = FieldType.Date)
    private Instant consultationDate;
}
