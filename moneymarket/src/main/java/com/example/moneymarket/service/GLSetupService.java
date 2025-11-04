package com.example.moneymarket.service;

import com.example.moneymarket.dto.GLSetupResponseDTO;
import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.repository.GLSetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for GL Setup operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GLSetupService {

    private final GLSetupRepository glSetupRepository;

    /**
     * Get all GL setups by layer ID
     * 
     * @param layerId The layer ID
     * @return List of GL setup responses
     */
    public List<GLSetupResponseDTO> getGLSetupsByLayerId(Integer layerId) {
        List<GLSetup> glSetups = glSetupRepository.findByLayerId(layerId);
        return glSetups.stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get GL setups by layer ID and parent GL number
     * 
     * @param layerId The layer ID
     * @param parentGlNum The parent GL number
     * @return List of GL setup responses
     */
    public List<GLSetupResponseDTO> getGLSetupsByLayerIdAndParent(Integer layerId, String parentGlNum) {
        List<GLSetup> glSetups = glSetupRepository.findByLayerId(layerId);
        return glSetups.stream()
                .filter(glSetup -> parentGlNum.equals(glSetup.getParentGLNum()))
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get Layer 4 GL numbers for interest payable and income accounts (for liabilities)
     *
     * Filters Layer 4 GL accounts that start with:
     * - 13: Interest Payable (for liability accounts)
     * - 14: Interest Income (for liability accounts)
     *
     * @return List of Layer 4 GL setup responses for payable and income accounts
     */
    public List<GLSetupResponseDTO> getInterestPayableReceivableLayer4GLs() {
        List<GLSetup> layer4GLs = glSetupRepository.findByLayerId(4);
        return layer4GLs.stream()
                .filter(glSetup -> {
                    String glNum = glSetup.getGlNum();
                    // Filter GL numbers starting with 13 (Payable) or 14 (Income) for liabilities
                    return glNum != null && (glNum.startsWith("13") || glNum.startsWith("14"));
                })
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Get Layer 4 GL numbers for interest expenditure and receivable accounts (for assets)
     *
     * Filters Layer 4 GL accounts that start with:
     * - 23: Interest Receivable (for asset accounts)
     * - 24: Interest Income (for asset accounts)
     *
     * @return List of Layer 4 GL setup responses for expenditure and receivable accounts
     */
    public List<GLSetupResponseDTO> getInterestIncomeExpenditureLayer4GLs() {
        List<GLSetup> layer4GLs = glSetupRepository.findByLayerId(4);
        return layer4GLs.stream()
                .filter(glSetup -> {
                    String glNum = glSetup.getGlNum();
                    // Filter GL numbers starting with 23 (Receivable) or 24 (Income) for assets
                    return glNum != null && (glNum.startsWith("23") || glNum.startsWith("24"));
                })
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Map entity to response DTO
     * 
     * @param entity The entity
     * @return The response DTO
     */
    private GLSetupResponseDTO mapToResponse(GLSetup entity) {
        return GLSetupResponseDTO.builder()
                .glName(entity.getGlName())
                .layerId(entity.getLayerId())
                .layerGLNum(entity.getLayerGLNum())
                .parentGLNum(entity.getParentGLNum())
                .glNum(entity.getGlNum())
                .build();
    }
}
