package com.example.moneymarket.service;

import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.GLSetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for GL hierarchy operations
 * Provides methods to traverse GL hierarchy and determine parent-child relationships
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GLHierarchyService {

    private final GLSetupRepository glSetupRepository;

    /**
     * Get the Layer 3 parent GL_Num for a given GL_Num
     * Traverses up the hierarchy until it finds a Layer 3 GL
     * 
     * @param glNum The GL_Num to find the Layer 3 parent for
     * @return The Layer 3 parent GL_Num, or null if not found
     */
    public String getLayer3ParentGLNum(String glNum) {
        if (glNum == null || glNum.isEmpty()) {
            return null;
        }

        GLSetup currentGL = glSetupRepository.findById(glNum)
                .orElseThrow(() -> new BusinessException("GL Number " + glNum + " does not exist"));

        // If current GL is already Layer 3, return it
        if (currentGL.getLayerId() == 3) {
            return glNum;
        }

        // Traverse up the hierarchy to find Layer 3 parent
        String parentGLNum = currentGL.getParentGLNum();
        while (parentGLNum != null && !parentGLNum.isEmpty()) {
            final String currentParentGLNum = parentGLNum; // Create effectively final variable for lambda
            GLSetup parentGL = glSetupRepository.findById(currentParentGLNum)
                    .orElseThrow(() -> new BusinessException("Parent GL Number " + currentParentGLNum + " does not exist"));
            
            if (parentGL.getLayerId() == 3) {
                return parentGLNum;
            }
            
            parentGLNum = parentGL.getParentGLNum();
        }

        return null;
    }

    /**
     * Check if a GL_Num belongs to overdraft accounts
     * Returns true if the Layer 3 parent GL_Num is 210201000 (Overdraft) or 140101000 (Overdraft Interest Income)
     * 
     * @param glNum The GL_Num to check
     * @return true if it's an overdraft account, false otherwise
     */
    public boolean isOverdraftAccount(String glNum) {
        String layer3ParentGLNum = getLayer3ParentGLNum(glNum);
        
        if (layer3ParentGLNum == null) {
            return false;
        }

        return "210201000".equals(layer3ParentGLNum) || "140101000".equals(layer3ParentGLNum);
    }

    /**
     * Get the full hierarchy path for a GL_Num
     * 
     * @param glNum The GL_Num to get hierarchy for
     * @return List of GL_Num strings from root to the specified GL_Num
     */
    public List<String> getGLHierarchyPath(String glNum) {
        if (glNum == null || glNum.isEmpty()) {
            return List.of();
        }

        List<String> hierarchy = new java.util.ArrayList<>();
        hierarchy.add(glNum);

        GLSetup currentGL = glSetupRepository.findById(glNum)
                .orElseThrow(() -> new BusinessException("GL Number " + glNum + " does not exist"));

        String parentGLNum = currentGL.getParentGLNum();
        while (parentGLNum != null && !parentGLNum.isEmpty()) {
            hierarchy.add(0, parentGLNum);
            
            final String currentParentGLNum = parentGLNum; // Create effectively final variable for lambda
            GLSetup parentGL = glSetupRepository.findById(currentParentGLNum)
                    .orElseThrow(() -> new BusinessException("Parent GL Number " + currentParentGLNum + " does not exist"));
            
            parentGLNum = parentGL.getParentGLNum();
        }

        return hierarchy;
    }

    /**
     * Check if a GL_Num is specifically an overdraft account (210201000)
     * 
     * @param glNum The GL_Num to check
     * @return true if it's specifically an overdraft account, false otherwise
     */
    public boolean isOverdraftAccountType(String glNum) {
        String layer3ParentGLNum = getLayer3ParentGLNum(glNum);
        return "210201000".equals(layer3ParentGLNum);
    }

    /**
     * Check if a GL_Num is specifically an overdraft interest income account (140101000)
     * 
     * @param glNum The GL_Num to check
     * @return true if it's specifically an overdraft interest income account, false otherwise
     */
    public boolean isOverdraftInterestIncomeAccount(String glNum) {
        String layer3ParentGLNum = getLayer3ParentGLNum(glNum);
        return "140101000".equals(layer3ParentGLNum);
    }
}
