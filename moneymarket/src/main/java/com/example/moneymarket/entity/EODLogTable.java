package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "EOD_Log_Table")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EODLogTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "EOD_Log_Id")
    private Long eodLogId;

    @Column(name = "EOD_Date", nullable = false)
    private LocalDate eodDate;

    @Column(name = "Job_Name", nullable = false, length = 50)
    private String jobName;

    @Column(name = "Start_Timestamp", nullable = false)
    private LocalDateTime startTimestamp;

    @Column(name = "End_Timestamp")
    private LocalDateTime endTimestamp;

    @Column(name = "System_Date", nullable = false)
    private LocalDate systemDate;

    @Column(name = "User_ID", nullable = false, length = 20)
    private String userId;

    @Column(name = "Records_Processed")
    @Builder.Default
    private Integer recordsProcessed = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "Status", nullable = false)
    @Builder.Default
    private EODStatus status = EODStatus.Running;

    @Column(name = "Error_Message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "Failed_At_Step", length = 500)
    private String failedAtStep;

    @Column(name = "Created_Timestamp", nullable = false)
    private LocalDateTime createdTimestamp;

    public enum EODStatus {
        Running, Success, Failed
    }
}
