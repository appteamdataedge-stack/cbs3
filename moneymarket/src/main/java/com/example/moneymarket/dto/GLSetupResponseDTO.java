package com.example.moneymarket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GL Setup response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GLSetupResponseDTO {
    
    private String glName;
    private Integer layerId;
    private String layerGLNum;
    private String parentGLNum;
    private String glNum;
}
