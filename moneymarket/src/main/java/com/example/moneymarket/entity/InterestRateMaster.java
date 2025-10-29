package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "Interest_Rate_Master")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterestRateMaster {

    @Id
    @Column(name = "Intt_Code", length = 20)
    private String inttCode;

    @Column(name = "Intt_Rate", nullable = false, precision = 5, scale = 2)
    private java.math.BigDecimal inttRate;

    @Column(name = "Intt_Effctv_Date", nullable = false)
    private LocalDate inttEffctvDate;
}


