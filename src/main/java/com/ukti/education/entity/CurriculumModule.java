package com.ukti.education.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "curriculum_modules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumModule {

    @Id
    @Column(name = "module_id", length = 100)
    private String moduleId;

    @Column(name = "class_level", nullable = false, length = 20)
    private String classLevel;

    @Column(columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode payload;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
