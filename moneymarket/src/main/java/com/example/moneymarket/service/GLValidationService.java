package com.example.moneymarket.service;

import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.entity.ProdMaster;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.repository.GLSetupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for GL mapping and validation
 * 
 * Enforces the following rules:
 * - Products always at Layer 3, Sub-Products always at Layer 4
 * - Maintain parent-child consistency between Product (Layer 3) and Sub-Product (Layer 4)
 * - Each account must be mapped to the correct Sub-Product GL_Num
 * - Enforce uniqueness: Layer_GL_Num must be unique, GL_Num unique, one GL_Name cannot have two Parent_GL_Num
 * - Enforce field lengths:
 *   - Layer_Id length 1
 *   - Layer_GL_Num length: L0=9, L1=8, L2=7, L3=5, L4=3
 *   - Parent_GL_Num = 9 digits, GL_Num = 9 digits
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GLValidationService {

    private final GLSetupRepository glSetupRepository;

    /**
     * Validate GL mapping for a product
     * 
     * @param product The product to validate
     * @throws BusinessException if validation fails
     */
    public void validateProductGLMapping(ProdMaster product) {
        if (product == null || product.getCumGLNum() == null) {
            throw new BusinessException("Product GL number cannot be null");
        }
        
        String glNum = product.getCumGLNum();
        
        // Validate GL exists
        GLSetup glSetup = glSetupRepository.findById(glNum)
                .orElseThrow(() -> new BusinessException("GL number " + glNum + " does not exist"));
        
        // Validate it's a Layer 3 GL
        if (glSetup.getLayerId() != 3) {
            throw new BusinessException("Product GL must be at Layer 3, but GL " + glNum + " is at Layer " + glSetup.getLayerId());
        }
        
        // Validate GL number length
        if (glSetup.getLayerGLNum().length() != 5) {
            throw new BusinessException("Layer 3 GL number must be 5 digits, but " + glSetup.getLayerGLNum() + " is " + 
                    glSetup.getLayerGLNum().length() + " digits");
        }
    }
    
    /**
     * Validate GL mapping for a sub-product
     * 
     * @param subProduct The sub-product to validate
     * @param product The parent product
     * @throws BusinessException if validation fails
     */
    public void validateSubProductGLMapping(SubProdMaster subProduct, ProdMaster product) {
        if (subProduct == null || subProduct.getCumGLNum() == null) {
            throw new BusinessException("Sub-product GL number cannot be null");
        }
        
        if (product == null || product.getCumGLNum() == null) {
            throw new BusinessException("Parent product GL number cannot be null");
        }
        
        String subProductGlNum = subProduct.getCumGLNum();
        String productGlNum = product.getCumGLNum();
        
        // Validate GL exists
        GLSetup glSetup = glSetupRepository.findById(subProductGlNum)
                .orElseThrow(() -> new BusinessException("GL number " + subProductGlNum + " does not exist"));
        
        // Validate it's a Layer 4 GL
        if (glSetup.getLayerId() != 4) {
            throw new BusinessException("Sub-product GL must be at Layer 4, but GL " + subProductGlNum + 
                    " is at Layer " + glSetup.getLayerId());
        }
        
        // Validate GL number length
        if (glSetup.getLayerGLNum().length() != 3) {
            throw new BusinessException("Layer 4 GL number must be 3 digits, but " + glSetup.getLayerGLNum() + 
                    " is " + glSetup.getLayerGLNum().length() + " digits");
        }
        
        // Validate parent-child relationship
        if (!glSetup.getParentGLNum().equals(productGlNum)) {
            throw new BusinessException("Sub-product GL " + subProductGlNum + 
                    " must have parent GL " + productGlNum + " but has " + glSetup.getParentGLNum());
        }
        
        // Note: GL uniqueness validation is NOT needed here because:
        // - Subproduct is referencing an EXISTING GL, not creating a new one
        // - GL uniqueness is enforced by the database primary key constraint on GL_Num
        // - The GL was already validated when it was created in GL_Setup
    }
    
    /**
     * Check if a GL number is a liability GL
     * Liability GL numbers start with 1
     * 
     * @param glNum The GL number to check
     * @return true if liability, false otherwise
     */
    public boolean isLiabilityGL(String glNum) {
        if (glNum == null || glNum.isEmpty()) {
            return false;
        }
        return glNum.startsWith("1");
    }
    
    /**
     * Check if a GL number is an asset GL
     * Asset GL numbers start with 2
     * 
     * @param glNum The GL number to check
     * @return true if asset, false otherwise
     */
    public boolean isAssetGL(String glNum) {
        if (glNum == null || glNum.isEmpty()) {
            return false;
        }
        return glNum.startsWith("2");
    }
    
    /**
     * Check if a GL number is for a customer account
     * Customer account GL numbers have 2nd digit = 1
     * 
     * @param glNum The GL number to check
     * @return true if customer account, false otherwise
     */
    public boolean isCustomerAccountGL(String glNum) {
        if (glNum == null || glNum.length() < 2) {
            return false;
        }
        return glNum.charAt(1) == '1';
    }
    
    /**
     * Check if a GL number is for an office account
     * Office account GL numbers have 2nd digit ≠ 1
     * 
     * @param glNum The GL number to check
     * @return true if office account, false otherwise
     */
    public boolean isOfficeAccountGL(String glNum) {
        if (glNum == null || glNum.length() < 2) {
            return false;
        }
        return glNum.charAt(1) != '1';
    }
    
    /**
     * Validate GL uniqueness constraints when CREATING a new GL
     * This should only be called when creating a new GL_Setup record, NOT when referencing existing GLs
     * 
     * @param glSetup The GL setup to validate
     * @param isNewGL Whether this is a new GL being created (true) or existing GL being referenced (false)
     * @throws BusinessException if validation fails
     */
    public void validateGLUniqueness(GLSetup glSetup, boolean isNewGL) {
        if (!isNewGL) {
            // Skip uniqueness validation if we're just referencing an existing GL
            return;
        }
        
        // Check if Layer_GL_Num is unique (only for new GLs)
        long countByLayerGlNum = glSetupRepository.countByLayerGLNum(glSetup.getLayerGLNum());
        if (countByLayerGlNum > 0) {
            throw new BusinessException("Layer_GL_Num " + glSetup.getLayerGLNum() + " is not unique");
        }
        
        // Check if GL_Num is unique (handled by primary key constraint)
        // No need to explicitly validate as database enforces this
        
        // Check if GL_Name with same Parent_GL_Num combination is unique
        List<GLSetup> existingGLs = glSetupRepository.findByGlName(glSetup.getGlName());
        for (GLSetup existing : existingGLs) {
            if (!existing.getGlNum().equals(glSetup.getGlNum()) && 
                existing.getParentGLNum() != null && 
                existing.getParentGLNum().equals(glSetup.getParentGLNum())) {
                throw new BusinessException("GL_Name " + glSetup.getGlName() + 
                        " with Parent_GL_Num " + glSetup.getParentGLNum() + " is not unique");
            }
        }
    }
    
    /**
     * Validate GL field lengths
     * 
     * @param glSetup The GL setup to validate
     * @throws BusinessException if validation fails
     */
    public void validateGLFieldLengths(GLSetup glSetup) {
        // Validate Layer_Id length (1 digit)
        if (glSetup.getLayerId() < 0 || glSetup.getLayerId() > 9) {
            throw new BusinessException("Layer_Id must be between 0 and 9");
        }
        
        // Validate Layer_GL_Num length based on Layer_Id
        int expectedLayerGLNumLength;
        switch (glSetup.getLayerId()) {
            case 0:
                expectedLayerGLNumLength = 9;
                break;
            case 1:
                expectedLayerGLNumLength = 8;
                break;
            case 2:
                expectedLayerGLNumLength = 7;
                break;
            case 3:
                expectedLayerGLNumLength = 5;
                break;
            case 4:
                expectedLayerGLNumLength = 3;
                break;
            default:
                throw new BusinessException("Invalid Layer_Id: " + glSetup.getLayerId());
        }
        
        if (glSetup.getLayerGLNum().length() != expectedLayerGLNumLength) {
            throw new BusinessException("Layer_GL_Num length must be " + expectedLayerGLNumLength + 
                    " for Layer " + glSetup.getLayerId() + ", but is " + glSetup.getLayerGLNum().length());
        }
        
        // Validate Parent_GL_Num length (9 digits)
        if (glSetup.getParentGLNum() != null && glSetup.getParentGLNum().length() != 9) {
            throw new BusinessException("Parent_GL_Num must be 9 digits, but is " + glSetup.getParentGLNum().length());
        }
        
        // Validate GL_Num length (9 digits)
        if (glSetup.getGlNum().length() != 9) {
            throw new BusinessException("GL_Num must be 9 digits, but is " + glSetup.getGlNum().length());
        }
    }
    
    /**
     * Get GL type based on first digit
     * 
     * @param glNum The GL number
     * @return The GL type (Asset, Liability, Equity, Income, Expense)
     */
    public String getGLType(String glNum) {
        if (glNum == null || glNum.isEmpty()) {
            return "Unknown";
        }
        
        char firstDigit = glNum.charAt(0);
        switch (firstDigit) {
            case '1':
                return "Asset";
            case '2':
                return "Liability";
            case '3':
                return "Equity";
            case '4':
                return "Income";
            case '5':
                return "Expense";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Validate parent-child GL hierarchy consistency
     * 
     * @param childGlNum The child GL number
     * @param parentGlNum The parent GL number
     * @throws BusinessException if validation fails
     */
    public void validateGLHierarchy(String childGlNum, String parentGlNum) {
        if (childGlNum == null || parentGlNum == null) {
            throw new BusinessException("Child and parent GL numbers cannot be null");
        }
        
        // Both must exist
        GLSetup childGL = glSetupRepository.findById(childGlNum)
                .orElseThrow(() -> new BusinessException("Child GL " + childGlNum + " does not exist"));
        
        GLSetup parentGL = glSetupRepository.findById(parentGlNum)
                .orElseThrow(() -> new BusinessException("Parent GL " + parentGlNum + " does not exist"));
        
        // Parent layer must be less than child layer
        if (parentGL.getLayerId() >= childGL.getLayerId()) {
            throw new BusinessException("Parent GL layer (" + parentGL.getLayerId() + 
                    ") must be less than child GL layer (" + childGL.getLayerId() + ")");
        }
        
        // Child's parent_gl_num must match parent's gl_num
        if (!childGL.getParentGLNum().equals(parentGL.getGlNum())) {
            throw new BusinessException("Child GL's parent_gl_num (" + childGL.getParentGLNum() + 
                    ") does not match parent GL's gl_num (" + parentGL.getGlNum() + ")");
        }
    }
}
