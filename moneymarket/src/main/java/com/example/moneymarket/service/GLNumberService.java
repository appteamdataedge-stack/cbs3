package com.example.moneymarket.service;

import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.GLSetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for GL Number computation and validation
 * Implements GL Number generation by concatenating Layer_GL_Num values L0→L4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GLNumberService {

    private final GLSetupRepository glSetupRepository;

    /**
     * Computes a complete GL number by concatenating layer GL numbers for a given GL entry
     * 
     * @param glNum The GL number to compute the complete hierarchy for
     * @return The full GL number
     */
    public String computeGLNumber(String glNum) {
        GLSetup glSetup = glSetupRepository.findById(glNum)
                .orElseThrow(() -> new BusinessException("GL Number " + glNum + " does not exist"));

        // Start with the current GL's layer number
        StringBuilder computedGL = new StringBuilder(glSetup.getLayerGLNum());
        
        // If this is a layer 0 GL (root), just return its layer GL number
        if (glSetup.getLayerId() == 0) {
            return computedGL.toString();
        }

        // Build the GL hierarchy from bottom to top
        for (String parentGLNum = glSetup.getParentGLNum(); 
             parentGLNum != null && !parentGLNum.isEmpty();) {
            
            final String currentParentGLNum = parentGLNum; // Create effectively final variable for lambda
            GLSetup parentGL = glSetupRepository.findById(currentParentGLNum)
                    .orElseThrow(() -> new BusinessException("Parent GL Number " + currentParentGLNum + " does not exist"));
            
            // Insert the parent's layer GL number at the beginning
            computedGL.insert(0, parentGL.getLayerGLNum());
            
            // Move up the hierarchy
            parentGLNum = parentGL.getParentGLNum();
        }

        return computedGL.toString();
    }

    /**
     * Validates that a GL number exists and has the correct parent in the hierarchy
     * 
     * @param glNum The GL number to validate
     * @param expectedParentGLNum The expected parent GL number
     * @param expectedLayerId The expected layer ID
     * @return true if validation succeeds, throws exception otherwise
     */
    public boolean validateGLNumber(String glNum, String expectedParentGLNum, Integer expectedLayerId) {
        GLSetup glSetup = glSetupRepository.findById(glNum)
                .orElseThrow(() -> new BusinessException("GL Number " + glNum + " does not exist"));

        // Validate layer ID
        if (expectedLayerId != null && glSetup.getLayerId() != expectedLayerId) {
            throw new BusinessException("GL Number " + glNum + " is not at expected layer " + expectedLayerId + 
                                       " (found at layer " + glSetup.getLayerId() + ")");
        }

        // Validate parent relationship
        if (expectedParentGLNum != null) {
            if (glSetup.getParentGLNum() == null || !glSetup.getParentGLNum().equals(expectedParentGLNum)) {
                throw new BusinessException("GL Number " + glNum + " does not have expected parent " + expectedParentGLNum);
            }
        }

        return true;
    }

    /**
     * Retrieves the full GL hierarchy path for a given GL number
     * 
     * @param glNum The GL number to get the hierarchy for
     * @return List of GL numbers from root to the specified GL number
     */
    public List<GLSetup> getGLHierarchy(String glNum) {
        List<GLSetup> hierarchy = new ArrayList<>();
        
        // Get the starting GL
        GLSetup currentGL = glSetupRepository.findById(glNum)
                .orElseThrow(() -> new BusinessException("GL Number " + glNum + " does not exist"));
        
        // Add the current GL to the hierarchy
        hierarchy.add(currentGL);
        
        // Build the GL hierarchy from bottom to top
        for (String parentGLNum = currentGL.getParentGLNum(); 
             parentGLNum != null && !parentGLNum.isEmpty();) {
            
            final String currentParentGLNum = parentGLNum; // Create effectively final variable for lambda
            GLSetup parentGL = glSetupRepository.findById(currentParentGLNum)
                    .orElseThrow(() -> new BusinessException("Parent GL Number " + currentParentGLNum + " does not exist"));
            
            // Add the parent to the hierarchy
            hierarchy.add(0, parentGL);
            
            // Move up the hierarchy
            parentGLNum = parentGL.getParentGLNum();
        }
        
        return hierarchy;
    }
}
