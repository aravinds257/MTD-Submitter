package com.mtdsubmitter.model;

import com.mtdsubmitter.model.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quarterly_submissions")
public class QuarterlySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quarterly_period_id", nullable = false)
    private QuarterlyPeriod quarterlyPeriod;

    @Column(columnDefinition = "jsonb")
    private String submissionJson;

    @Column(columnDefinition = "jsonb")
    private String responseJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    private LocalDateTime submittedAt;

    private String hmrcSubmissionId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
