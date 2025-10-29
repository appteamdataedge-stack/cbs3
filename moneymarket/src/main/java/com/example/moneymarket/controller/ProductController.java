package com.example.moneymarket.controller;

import com.example.moneymarket.dto.CustomerVerificationDTO;
import com.example.moneymarket.dto.ProductRequestDTO;
import com.example.moneymarket.dto.ProductResponseDTO;
import com.example.moneymarket.dto.GLSetupResponseDTO;
import com.example.moneymarket.service.ProductService;
import com.example.moneymarket.service.GLSetupService;
import com.example.moneymarket.validation.ProductValidator;
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
 * REST controller for product operations
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductValidator productValidator;
    private final GLSetupService glSetupService;

    /**
     * Initialize validator for product request
     * 
     * @param binder The WebDataBinder
     */
    @InitBinder("productRequestDTO")
    public void initBinder(WebDataBinder binder) {
        binder.addValidators(productValidator);
    }

    /**
     * Create a new product
     * 
     * @param productRequestDTO The product data
     * @param bindingResult Validation result
     * @return The created product
     */
    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO productRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        ProductResponseDTO createdProduct = productService.createProduct(productRequestDTO);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    /**
     * Update an existing product
     * 
     * @param id The product ID
     * @param productRequestDTO The product data
     * @param bindingResult Validation result
     * @return The updated product
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequestDTO productRequestDTO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new com.example.moneymarket.exception.BusinessException(
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        
        ProductResponseDTO updatedProduct = productService.updateProduct(id, productRequestDTO);
        return ResponseEntity.ok(updatedProduct);
    }

    /**
     * Get a product by ID
     * 
     * @param id The product ID
     * @return The product
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> getProduct(@PathVariable Integer id) {
        ProductResponseDTO product = productService.getProduct(id);
        return ResponseEntity.ok(product);
    }

    /**
     * Get all products with pagination
     * 
     * @param pageable The pagination information
     * @return Page of products
     */
    @GetMapping
    public ResponseEntity<Page<ProductResponseDTO>> getAllProducts(Pageable pageable) {
        Page<ProductResponseDTO> products = productService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Get all Layer 3 GL entries for product dropdown
     * 
     * @return List of Layer 3 GL entries
     */
    @GetMapping("/gl-options")
    public ResponseEntity<List<GLSetupResponseDTO>> getProductGLOptions() {
        List<GLSetupResponseDTO> glOptions = glSetupService.getGLSetupsByLayerId(3);
        return ResponseEntity.ok(glOptions);
    }

    /**
     * Verify a product
     * 
     * @param id The product ID
     * @param verificationDTO The verification data
     * @return The verified product
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<ProductResponseDTO> verifyProduct(
            @PathVariable Integer id,
            @Valid @RequestBody CustomerVerificationDTO verificationDTO) {
        ProductResponseDTO verifiedProduct = productService.verifyProduct(id, verificationDTO);
        return ResponseEntity.ok(verifiedProduct);
    }

    /**
     * Get customer products (filtered by customer account GL numbers)
     * 
     * @param pageable The pagination information
     * @return Page of customer products
     */
    @GetMapping("/customer-products")
    public ResponseEntity<Page<ProductResponseDTO>> getCustomerProducts(Pageable pageable) {
        Page<ProductResponseDTO> products = productService.getCustomerProducts(pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Get products filtered by account type (liability/asset)
     * 
     * @param accountType The account type (LIABILITY/ASSET)
     * @param pageable The pagination information
     * @return Page of filtered products
     */
    @GetMapping("/by-account-type/{accountType}")
    public ResponseEntity<Page<ProductResponseDTO>> getProductsByAccountType(
            @PathVariable String accountType,
            Pageable pageable) {
        Page<ProductResponseDTO> products = productService.getProductsByAccountType(accountType, pageable);
        return ResponseEntity.ok(products);
    }
}
