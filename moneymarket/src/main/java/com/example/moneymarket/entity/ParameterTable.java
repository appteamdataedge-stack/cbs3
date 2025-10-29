package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Parameter_Table")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Parameter_Id")
    private Integer parameterId;

    @Column(name = "Parameter_Name", nullable = false, unique = true, length = 50)
    private String parameterName;

    @Column(name = "Parameter_Value", nullable = false, length = 100)
    private String parameterValue;

    @Column(name = "Parameter_Description", length = 200)
    private String parameterDescription;

    @Column(name = "Last_Updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "Updated_By", nullable = false, length = 20)
    private String updatedBy;
}
