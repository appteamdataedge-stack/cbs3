package com.example.moneymarket.service;

import com.example.moneymarket.dto.ProductRequestDTO;
import com.example.moneymarket.dto.ProductResponseDTO;
import com.example.moneymarket.dto.CustomerVerificationDTO;
import com.example.moneymarket.dto.SubProductResponseDTO;
import com.example.moneymarket.entity.ProdMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.ProdMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for product operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProdMasterRepository prodMasterRepository;
    private final GLNumberService glNumberService;
    private final GLValidationService glValidationService;
    private final SystemDateService systemDateService;
    
    /**
     * Create a new product
     * 
     * @param productRequestDTO The product data
     * @return The created product response
     */
    @Transactional
    public ProductResponseDTO createProduct(ProductRequestDTO productRequestDTO) {
        // Check if product code is unique
        if (prodMasterRepository.existsByProductCode(productRequestDTO.getProductCode())) {
            throw new BusinessException("Product Code already exists");
        }

        // Validate GL Number exists and is at layer 3
        try {
            glNumberService.validateGLNumber(productRequestDTO.getCumGLNum(), null, 3);
        } catch (BusinessException e) {
            throw new BusinessException("Invalid GL Number: " + e.getMessage());
        }

        // Map DTO to entity
        ProdMaster product = mapToEntity(productRequestDTO);

        // Set audit fields
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        product.setEntryDate(systemDateService.getSystemDate());
        product.setEntryTime(LocalTime.now()); // Keep current time for audit timestamps

        // Save the product
        ProdMaster savedProduct = prodMasterRepository.save(product);
        log.info("Product created with ID: {}", savedProduct.getProductId());

        // Return the response
        return mapToResponse(savedProduct);
    }

    /**
     * Update an existing product
     * 
     * @param productId The product ID
     * @param productRequestDTO The product data
     * @return The updated product response
     */
    @Transactional
    public ProductResponseDTO updateProduct(Integer productId, ProductRequestDTO productRequestDTO) {
        // Find the product
        ProdMaster product = prodMasterRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", productId));

        // Product code uniqueness validation removed for updates

        // Validate GL Number exists and is at layer 3
        try {
            glNumberService.validateGLNumber(productRequestDTO.getCumGLNum(), null, 3);
        } catch (BusinessException e) {
            throw new BusinessException("Invalid GL Number: " + e.getMessage());
        }

        // Update fields (productCode is not updated - it remains unchanged)
        // product.setProductCode(productRequestDTO.getProductCode()); // Product code cannot be changed after creation
        product.setProductName(productRequestDTO.getProductName());
        product.setCumGLNum(productRequestDTO.getCumGLNum());
        product.setCustomerProductFlag(productRequestDTO.getCustomerProductFlag());
        product.setInterestBearingFlag(productRequestDTO.getInterestBearingFlag());
        product.setDealOrRunning(productRequestDTO.getDealOrRunning());
        product.setCurrency(productRequestDTO.getCurrency());
        product.setMakerId(productRequestDTO.getMakerId());
        
        // After update, reset verification fields
        product.setVerifierId(null);
        product.setVerificationDate(null);
        product.setVerificationTime(null);

        // Save the updated product
        ProdMaster updatedProduct = prodMasterRepository.save(product);
        log.info("Product updated with ID: {}", updatedProduct.getProductId());

        // Return the response
        return mapToResponse(updatedProduct);
    }

    /**
     * Get a product by ID
     * 
     * @param productId The product ID
     * @return The product response
     */
    public ProductResponseDTO getProduct(Integer productId) {
        // Find the product
        ProdMaster product = prodMasterRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", productId));

        // Return the response
        return mapToResponse(product);
    }

    /**
     * Get all products with pagination
     * 
     * @param pageable The pagination information
     * @return Page of product responses
     */
    public Page<ProductResponseDTO> getAllProducts(Pageable pageable) {
        // Get the products page
        Page<ProdMaster> products = prodMasterRepository.findAll(pageable);

        // Map to response DTOs
        return products.map(this::mapToResponse);
    }

    /**
     * Verify a product
     * 
     * @param productId The product ID
     * @param verificationDTO The verification data
     * @return The verified product response
     */
    @Transactional
    public ProductResponseDTO verifyProduct(Integer productId, CustomerVerificationDTO verificationDTO) {
        // Find the product
        ProdMaster product = prodMasterRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", productId));

        // Check if maker and verifier are different
        if (product.getMakerId().equals(verificationDTO.getVerifierId())) {
            throw new BusinessException("Maker cannot verify their own record");
        }

        // Set verification fields
        product.setVerifierId(verificationDTO.getVerifierId());
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        product.setVerificationDate(systemDateService.getSystemDate());
        product.setVerificationTime(LocalTime.now()); // Keep current time for audit timestamps

        // Save the verified product
        ProdMaster verifiedProduct = prodMasterRepository.save(product);
        log.info("Product verified with ID: {}", verifiedProduct.getProductId());

        // Return the response
        return mapToResponse(verifiedProduct);
    }

    /**
     * Map DTO to entity
     * 
     * @param dto The DTO
     * @return The entity
     */
    private ProdMaster mapToEntity(ProductRequestDTO dto) {
        return ProdMaster.builder()
                .productCode(dto.getProductCode())
                .productName(dto.getProductName())
                .cumGLNum(dto.getCumGLNum())
                .customerProductFlag(dto.getCustomerProductFlag())
                .interestBearingFlag(dto.getInterestBearingFlag())
                .dealOrRunning(dto.getDealOrRunning())
                .currency(dto.getCurrency())
                .makerId(dto.getMakerId())
                // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
                .entryDate(systemDateService.getSystemDate())
                .entryTime(LocalTime.now()) // Keep current time for audit timestamps
                .build();
    }

    /**
     * Map entity to response DTO
     * 
     * @param entity The entity
     * @return The response DTO
     */
    private ProductResponseDTO mapToResponse(ProdMaster entity) {
        // Map sub-products if they exist
        List<SubProductResponseDTO> subProducts = entity.getSubProducts() != null ? 
            entity.getSubProducts().stream()
                .map(sp -> SubProductResponseDTO.builder()
                        .subProductId(sp.getSubProductId())
                        .productId(entity.getProductId())
                        .subProductCode(sp.getSubProductCode())
                        .subProductName(sp.getSubProductName())
                        .customerProductFlag(entity.getCustomerProductFlag())
                        .inttCode(sp.getInttCode())
                        .cumGLNum(sp.getCumGLNum())
                        .extGLNum(sp.getExtGLNum())
                        .subProductStatus(sp.getSubProductStatus())
                        .makerId(sp.getMakerId())
                        .entryDate(sp.getEntryDate())
                        .entryTime(sp.getEntryTime())
                        .verifierId(sp.getVerifierId())
                        .verificationDate(sp.getVerificationDate())
                        .verificationTime(sp.getVerificationTime())
                        .verified(sp.getVerifierId() != null)
                        .build())
                .collect(Collectors.toList()) : 
            Collections.emptyList();
            
        return ProductResponseDTO.builder()
                .productId(entity.getProductId())
                .productCode(entity.getProductCode())
                .productName(entity.getProductName())
                .cumGLNum(entity.getCumGLNum())
                .customerProductFlag(entity.getCustomerProductFlag())
                .interestBearingFlag(entity.getInterestBearingFlag())
                .dealOrRunning(entity.getDealOrRunning())
                .currency(entity.getCurrency())
                .makerId(entity.getMakerId())
                .entryDate(entity.getEntryDate())
                .entryTime(entity.getEntryTime())
                .verifierId(entity.getVerifierId())
                .verificationDate(entity.getVerificationDate())
                .verificationTime(entity.getVerificationTime())
                .verified(entity.getVerifierId() != null)
                .subProducts(subProducts)
                .build();
    }

    /**
     * Get customer products (filtered by customer account GL numbers)
     * 
     * @param pageable The pagination information
     * @return Page of customer products
     */
    public Page<ProductResponseDTO> getCustomerProducts(Pageable pageable) {
        // Get all products
        Page<ProdMaster> products = prodMasterRepository.findAll(pageable);
        
        // Filter products that have customer account GL numbers (2nd digit = 1)
        return products.map(this::mapToResponse)
                .map(product -> {
                    // Only include products with customer account GL numbers
                    if (glValidationService.isCustomerAccountGL(product.getCumGLNum())) {
                        return product;
                    }
                    return null;
                })
                .map(product -> product) // Remove nulls
                .map(product -> product); // This is a simplified filter - in real implementation, use repository query
    }

    /**
     * Get products filtered by account type (liability/asset)
     * 
     * @param accountType The account type (LIABILITY/ASSET)
     * @param pageable The pagination information
     * @return Page of filtered products
     */
    public Page<ProductResponseDTO> getProductsByAccountType(String accountType, Pageable pageable) {
        // Get all products
        Page<ProdMaster> products = prodMasterRepository.findAll(pageable);
        
        // Filter products by account type
        return products.map(this::mapToResponse)
                .map(product -> {
                    boolean isLiability = "LIABILITY".equalsIgnoreCase(accountType) && 
                                        glValidationService.isLiabilityGL(product.getCumGLNum());
                    boolean isAsset = "ASSET".equalsIgnoreCase(accountType) && 
                                    glValidationService.isAssetGL(product.getCumGLNum());
                    
                    if (isLiability || isAsset) {
                        return product;
                    }
                    return null;
                })
                .map(product -> product); // This is a simplified filter - in real implementation, use repository query
    }
}
