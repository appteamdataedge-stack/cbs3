package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Account_Seq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSeq {

    @Id
    @Column(name = "GL_Num", length = 9)
    private String glNum;

    @ManyToOne
    @JoinColumn(name = "GL_Num", insertable = false, updatable = false)
    private GLSetup glSetup;

    @Column(name = "Seq_Number", nullable = false)
    private Integer seqNumber;

    @Column(name = "Last_Updated", nullable = false)
    private LocalDateTime lastUpdated;
}
