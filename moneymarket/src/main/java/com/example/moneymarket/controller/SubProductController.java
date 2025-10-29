package com.example.moneymarket.controller;

import com.example.moneymarket.dto.CustomerVerificationDTO;
import com.example.moneymarket.dto.SubProductRequestDTO;
import com.example.moneymarket.dto.SubProductResponseDTO;
import com.example.moneymarket.dto.GLSetupResponseDTO;
import com.example.moneymarket.service.SubProductService;
import com.example.moneymarket.service.GLSetupService;
import com.example.moneymarket.validation.SubProductValidator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for sub-product operations
 */
@RestController
@RequestMapping("/api/subproducts")
@RequiredArgsConstructor
public class SubProductController {

    private final SubProductService subProductService;
    private final SubProductValidator subProductValidator;
    private final GLSetupService glSetupService;

    /**
     * Initialize validator for sub-product request
     * 
     * @param binder The WebDataBinder
     */
    @InitBinder("subProductRequestDTO")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(subProductValidator);
    }

    /**
     * Create a new sub-product
     * 
     * @param subProductRequestDTO The sub-product data
     * @param bindingResult Validation result
     * @return The created sub-product
     */
    @PostMapping
    public ResponseEntity<SubProductResponseDTO> createSubProduct(
            @Valid @RequestBody SubProductRequestDTO subProductRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        SubProductResponseDTO createdSubProduct = subProductService.createSubProduct(subProductRequestDTO);
        return new ResponseEntity<>(createdSubProduct, HttpStatus.CREATED);
    }

    /**
     * Update an existing sub-product
     * 
     * @param id The sub-product ID
     * @param subProductRequestDTO The sub-product data
     * @param bindingResult Validation result
     * @return The updated sub-product
     */
    @PutMapping("/{id}")
    public ResponseEntity<SubProductResponseDTO> updateSubProduct(
            @PathVariable Integer id,
            @Valid @RequestBody SubProductRequestDTO subProductRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        SubProductResponseDTO updatedSubProduct = subProductService.updateSubProduct(id, subProductRequestDTO);
        return ResponseEntity.ok(updatedSubProduct);
    }

    /**
     * Get a sub-product by ID
     * 
     * @param id The sub-product ID
     * @return The sub-product
     */
    @GetMapping("/{id}")
    public ResponseEntity<SubProductResponseDTO> getSubProduct(@PathVariable Integer id) {
        SubProductResponseDTO subProduct = subProductService.getSubProduct(id);
        return ResponseEntity.ok(subProduct);
    }

    /**
     * Get all sub-products with pagination
     * 
     * @param pageable The pagination information
     * @return Page of sub-products
     */
    @GetMapping
    public ResponseEntity<Page<SubProductResponseDTO>> getAllSubProducts(Pageable pageable) {
        Page<SubProductResponseDTO> subProducts = subProductService.getAllSubProducts(pageable);
        return ResponseEntity.ok(subProducts);
    }

    /**
     * Get all Layer 4 GL entries for sub-product dropdown
     * 
     * @return List of Layer 4 GL entries
     */
    @GetMapping("/gl-options")
    public ResponseEntity<List<GLSetupResponseDTO>> getSubProductGLOptions() {
        List<GLSetupResponseDTO> glOptions = glSetupService.getGLSetupsByLayerId(4);
        return ResponseEntity.ok(glOptions);
    }

    /**
     * Get Layer 4 GL entries filtered by parent GL number
     * 
     * @param parentGlNum The parent GL number
     * @return List of Layer 4 GL entries filtered by parent
     */
    @GetMapping("/gl-options/{parentGlNum}")
    public ResponseEntity<List<GLSetupResponseDTO>> getSubProductGLOptionsByParent(
            @PathVariable String parentGlNum) {
        List<GLSetupResponseDTO> glOptions = glSetupService.getGLSetupsByLayerIdAndParent(4, parentGlNum);
        return ResponseEntity.ok(glOptions);
    }

    /**
     * Verify a sub-product
     * 
     * @param id The sub-product ID
     * @param verificationDTO The verification data
     * @return The verified sub-product
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<SubProductResponseDTO> verifySubProduct(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerVerificationDTO verificationDTO) {
        SubProductResponseDTO verifiedSubProduct = subProductService.verifySubProduct(id, verificationDTO);
        return ResponseEntity.ok(verifiedSubProduct);
    }

    /**
     * Get customer sub-products (filtered by customer account GL numbers)
     * 
     * @param pageable The pagination information
     * @return Page of customer sub-products
     */
    @GetMapping("/customer-subproducts")
    public ResponseEntity<Page<SubProductResponseDTO>> getCustomerSubProducts(Pageable pageable) {
        Page<SubProductResponseDTO> subProducts = subProductService.getCustomerSubProducts(pageable);
        return ResponseEntity.ok(subProducts);
    }

    /**
     * Get sub-products filtered by account type (liability/asset)
     * 
     * @param accountType The account type (LIABILITY/ASSET)
     * @param pageable The pagination information
     * @return Page of filtered sub-products
     */
    @GetMapping("/by-account-type/{accountType}")
    public ResponseEntity<Page<SubProductResponseDTO>> getSubProductsByAccountType(
            @PathVariable String accountType,
            Pageable pageable) {
        Page<SubProductResponseDTO> subProducts = subProductService.getSubProductsByAccountType(accountType, pageable);
        return ResponseEntity.ok(subProducts);
    }

    /**
     * Get sub-products filtered by product ID and account type
     * 
     * @param productId The product ID
     * @param accountType The account type (LIABILITY/ASSET)
     * @param pageable The pagination information
     * @return Page of filtered sub-products
     */
    @GetMapping("/by-product-and-type/{productId}/{accountType}")
    public ResponseEntity<Page<SubProductResponseDTO>> getSubProductsByProductAndType(
            @PathVariable Integer productId,
            @PathVariable String accountType,
            Pageable pageable) {
        Page<SubProductResponseDTO> subProducts = subProductService.getSubProductsByProductAndType(productId, accountType, pageable);
        return ResponseEntity.ok(subProducts);
    }
}
