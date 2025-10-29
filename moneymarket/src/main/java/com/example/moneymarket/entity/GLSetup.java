package com.example.moneymarket.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "GL_setup")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLSetup {

    @Column(name = "GL_Name", length = 50)
    private String glName;

    @Column(name = "Layer_Id")
    private Integer layerId;

    @Column(name = "Layer_GL_Num", length = 9)
    private String layerGLNum;

    @Column(name = "Parent_GL_Num", length = 9)
    private String parentGLNum;

    @Id
    @Column(name = "GL_Num", length = 9)
    private String glNum;

    /**
     * FIX (2025-10-23): Changed from @OneToOne to @OneToMany
     *
     * REASON: One GL account can have MULTIPLE balance records (one per transaction date).
     * Using @OneToOne caused "Duplicate row was found" errors when Hibernate tried to
     * load GLSetup and found multiple GLBalance records for the same GL_Num.
     *
     * SOLUTION: Use @OneToMany with LAZY fetch to prevent automatic loading.
     * Most operations don't need the balance history, so we avoid loading it by default.
     */
    @OneToMany(mappedBy = "glSetup", fetch = FetchType.LAZY)
    private List<GLBalance> balances;
}
