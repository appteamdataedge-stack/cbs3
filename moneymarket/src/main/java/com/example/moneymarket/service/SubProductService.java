package com.example.moneymarket.service;

import com.example.moneymarket.dto.SubProductRequestDTO;
import com.example.moneymarket.dto.SubProductResponseDTO;
import com.example.moneymarket.dto.CustomerVerificationDTO;
import com.example.moneymarket.entity.GLBalance;
import com.example.moneymarket.entity.ProdMaster;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.exception.BusinessException;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.GLBalanceRepository;
import com.example.moneymarket.repository.ProdMasterRepository;
import com.example.moneymarket.repository.InterestRateMasterRepository;
import com.example.moneymarket.repository.SubProdMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Service for sub-product operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubProductService {

    private final SubProdMasterRepository subProdMasterRepository;
    private final ProdMasterRepository prodMasterRepository;
    private final GLBalanceRepository glBalanceRepository;
    private final GLNumberService glNumberService;
    private final GLValidationService glValidationService;
    private final InterestRateMasterRepository interestRateMasterRepository;
    private final SystemDateService systemDateService;

    /**
     * Create a new sub-product
     * 
     * @param subProductRequestDTO The sub-product data
     * @return The created sub-product response
     */
    @Transactional
    public SubProductResponseDTO createSubProduct(SubProductRequestDTO subProductRequestDTO) {
        log.info("=== CREATE SUB-PRODUCT SERVICE CALL ===");
        log.info("Received DTO: {}", subProductRequestDTO);
        log.info("Interest Code: {}", subProductRequestDTO.getInttCode());
        log.info("Interest Increment: {}", subProductRequestDTO.getInterestIncrement());
        
        // Check if sub-product code is unique
        if (subProdMasterRepository.existsBySubProductCode(subProductRequestDTO.getSubProductCode())) {
            throw new BusinessException("Sub-Product Code already exists");
        }

        // Validate product exists
        ProdMaster product = prodMasterRepository.findById(subProductRequestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", subProductRequestDTO.getProductId()));
        
        log.info("Product found: ID={}, customerProduct={}, interestBearing={}", 
                product.getProductId(), product.getCustomerProductFlag(), product.getInterestBearingFlag());

        // Validate GL Number exists and is at layer 4
        try {
            glNumberService.validateGLNumber(subProductRequestDTO.getCumGLNum(), product.getCumGLNum(), 4);
            
            // Create a temporary entity for validation
            SubProdMaster tempSubProduct = mapToEntity(subProductRequestDTO, product);
            
            // Additional validation for GL mapping
            glValidationService.validateSubProductGLMapping(tempSubProduct, product);
        } catch (BusinessException e) {
            throw new BusinessException("Invalid GL Number: " + e.getMessage());
        }
        
        // Validate Interest Code and External GL Num based on product type
        // Handle both Boolean objects and primitive boolean values from database (1/0)
        boolean isCustomerProduct = product.getCustomerProductFlag() != null && product.getCustomerProductFlag();
        boolean isInterestBearing = product.getInterestBearingFlag() != null && product.getInterestBearingFlag();
        boolean isCustomerInterestBearing = isCustomerProduct && isInterestBearing;
        
        log.info("Product validation flags - Customer: {}, Interest: {}, Both: {}", 
                isCustomerProduct, isInterestBearing, isCustomerInterestBearing);
        
        // Interest Code is only required for customer+interest bearing products
        if (isCustomerInterestBearing) {
            if (subProductRequestDTO.getInttCode() == null || subProductRequestDTO.getInttCode().trim().isEmpty()) {
                throw new BusinessException("Interest Code is required for customer interest-bearing products");
            }
            
            // Interest increment is also required for customer+interest bearing products
            if (subProductRequestDTO.getInterestIncrement() == null) {
                throw new BusinessException("Interest Increment is required for customer interest-bearing products");
            }
            
            // Interest GL fields validation: at least ONE consolidated GL must be configured
            boolean hasReceivableExp = (subProductRequestDTO.getInterestReceivableExpenditureGLNum() != null &&
                                   !subProductRequestDTO.getInterestReceivableExpenditureGLNum().trim().isEmpty());
            boolean hasIncomePay = (subProductRequestDTO.getInterestIncomePayableGLNum() != null &&
                                 !subProductRequestDTO.getInterestIncomePayableGLNum().trim().isEmpty());

            if (!hasReceivableExp && !hasIncomePay) {
                throw new BusinessException("At least one interest GL field must be configured for customer interest-bearing products");
            }
        }
        
        // External GL Number is optional for all products (no validation required)

        // Map DTO to entity
        SubProdMaster subProduct = mapToEntity(subProductRequestDTO, product);

        // Compute effective interest rate if applicable
        applyEffectiveInterestRate(product, subProduct);

        // Set audit fields
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        subProduct.setEntryDate(systemDateService.getSystemDate());
        subProduct.setEntryTime(LocalTime.now()); // Keep current time for audit timestamps

        // Save the sub-product
        SubProdMaster savedSubProduct = subProdMasterRepository.save(subProduct);
        log.info("Sub-Product created with ID: {}", savedSubProduct.getSubProductId());

        // Return the response
        return mapToResponse(savedSubProduct);
    }

    /**
     * Update an existing sub-product
     * 
     * @param subProductId The sub-product ID
     * @param subProductRequestDTO The sub-product data
     * @return The updated sub-product response
     */
    @Transactional
    public SubProductResponseDTO updateSubProduct(Integer subProductId, SubProductRequestDTO subProductRequestDTO) {
        // Find the sub-product
        SubProdMaster subProduct = subProdMasterRepository.findById(subProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", subProductId));

        // Sub-product code uniqueness validation removed for updates

        // Validate product exists
        ProdMaster product = prodMasterRepository.findById(subProductRequestDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", subProductRequestDTO.getProductId()));

        // Validate GL Number exists and is at layer 4
        try {
            glNumberService.validateGLNumber(subProductRequestDTO.getCumGLNum(), product.getCumGLNum(), 4);
            
            // Create a temporary entity for validation
            SubProdMaster tempSubProduct = new SubProdMaster();
            tempSubProduct.setCumGLNum(subProductRequestDTO.getCumGLNum());
            
            // Additional validation for GL mapping
            glValidationService.validateSubProductGLMapping(tempSubProduct, product);
        } catch (BusinessException e) {
            throw new BusinessException("Invalid GL Number: " + e.getMessage());
        }

        // Validate Interest Code and External GL Num based on product type
        // Handle both Boolean objects and primitive boolean values from database (1/0)
        boolean isCustomerProduct = product.getCustomerProductFlag() != null && product.getCustomerProductFlag();
        boolean isInterestBearing = product.getInterestBearingFlag() != null && product.getInterestBearingFlag();
        boolean isCustomerInterestBearing = isCustomerProduct && isInterestBearing;
        
        log.info("Product validation flags (Update) - Customer: {}, Interest: {}, Both: {}", 
                isCustomerProduct, isInterestBearing, isCustomerInterestBearing);
        
        // Interest Code is only required for customer+interest bearing products
        if (isCustomerInterestBearing) {
            if (subProductRequestDTO.getInttCode() == null || subProductRequestDTO.getInttCode().trim().isEmpty()) {
                throw new BusinessException("Interest Code is required for customer interest-bearing products");
            }
            
            // Interest increment is also required for customer+interest bearing products
            if (subProductRequestDTO.getInterestIncrement() == null) {
                throw new BusinessException("Interest Increment is required for customer interest-bearing products");
            }
            
            // Interest GL fields validation: at least ONE consolidated GL must be configured
            boolean hasReceivableExp = (subProductRequestDTO.getInterestReceivableExpenditureGLNum() != null &&
                                   !subProductRequestDTO.getInterestReceivableExpenditureGLNum().trim().isEmpty());
            boolean hasIncomePay = (subProductRequestDTO.getInterestIncomePayableGLNum() != null &&
                                 !subProductRequestDTO.getInterestIncomePayableGLNum().trim().isEmpty());

            if (!hasReceivableExp && !hasIncomePay) {
                throw new BusinessException("At least one interest GL field must be configured for customer interest-bearing products");
            }
        }
        
        // External GL Number is optional for all products (no validation required)

        // Check GL balance for modifications
        Optional<GLBalance> glBalance = glBalanceRepository.findLatestByGlNum(subProduct.getCumGLNum());
        if (glBalance.isPresent() && glBalance.get().getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
            // If status changed to Deactive
            if (subProduct.getSubProductStatus() != SubProdMaster.SubProductStatus.Deactive && 
                subProductRequestDTO.getSubProductStatus() == SubProdMaster.SubProductStatus.Deactive) {
                throw new BusinessException("Cannot deactivate Sub-Product while GL balance is non-zero");
            }
            
            // Any modification when balance is non-zero
            if (!subProduct.getCumGLNum().equals(subProductRequestDTO.getCumGLNum())) {
                throw new BusinessException("Cannot modify GL Number while GL balance is non-zero");
            }
        }
        
        // Disallow reactivation of 'Deactive' sub-products
        if (subProduct.getSubProductStatus() == SubProdMaster.SubProductStatus.Deactive && 
            subProductRequestDTO.getSubProductStatus() != SubProdMaster.SubProductStatus.Deactive) {
            throw new BusinessException("Cannot reactivate a deactivated Sub-Product");
        }

        // Update fields
        subProduct.setProduct(product);
        subProduct.setSubProductCode(subProductRequestDTO.getSubProductCode());
        subProduct.setSubProductName(subProductRequestDTO.getSubProductName());
        subProduct.setInttCode(subProductRequestDTO.getInttCode());
        subProduct.setCumGLNum(subProductRequestDTO.getCumGLNum());
        subProduct.setExtGLNum(subProductRequestDTO.getExtGLNum());
        subProduct.setInterestIncrement(subProductRequestDTO.getInterestIncrement());
        subProduct.setInterestReceivableExpenditureGLNum(subProductRequestDTO.getInterestReceivableExpenditureGLNum());
        subProduct.setInterestIncomePayableGLNum(subProductRequestDTO.getInterestIncomePayableGLNum());
        subProduct.setSubProductStatus(subProductRequestDTO.getSubProductStatus());
        subProduct.setMakerId(subProductRequestDTO.getMakerId());
        
        // After update, reset verification fields
        subProduct.setVerifierId(null);
        subProduct.setVerificationDate(null);
        subProduct.setVerificationTime(null);

        // Recompute effective interest rate if applicable
        applyEffectiveInterestRate(product, subProduct);

        // Save the updated sub-product
        SubProdMaster updatedSubProduct = subProdMasterRepository.save(subProduct);
        log.info("Sub-Product updated with ID: {}", updatedSubProduct.getSubProductId());

        // Return the response
        return mapToResponse(updatedSubProduct);
    }

    private void applyEffectiveInterestRate(ProdMaster product, SubProdMaster subProduct) {
        log.info("=== APPLY EFFECTIVE INTEREST RATE ===");
        log.info("Product - customerProduct: {}, interestBearing: {}", 
                product.getCustomerProductFlag(), product.getInterestBearingFlag());
        log.info("SubProduct - inttCode: {}, interestIncrement: {}", 
                subProduct.getInttCode(), subProduct.getInterestIncrement());
        
        if (Boolean.TRUE.equals(product.getCustomerProductFlag()) && Boolean.TRUE.equals(product.getInterestBearingFlag())) {
            log.info("Product is customer+interest bearing, validating interest fields...");
            if (subProduct.getInttCode() == null || subProduct.getInterestIncrement() == null) {
                log.error("VALIDATION FAILED - inttCode: {}, interestIncrement: {}", 
                        subProduct.getInttCode(), subProduct.getInterestIncrement());
                throw new BusinessException("Interest Code and Interest Increment are required when product is interest-bearing");
            }
            java.math.BigDecimal baseRate = interestRateMasterRepository
                // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
                .findTopByInttCodeAndInttEffctvDateLessThanEqualOrderByInttEffctvDateDesc(subProduct.getInttCode(), systemDateService.getSystemDate())
                .map(r -> r.getInttRate())
                .orElseThrow(() -> new BusinessException("No interest rate found for code: " + subProduct.getInttCode()));
            java.math.BigDecimal effective = baseRate.add(subProduct.getInterestIncrement());
            subProduct.setEffectiveInterestRate(effective);
        } else {
            subProduct.setEffectiveInterestRate(null);
        }
    }

    /**
     * Get a sub-product by ID
     * 
     * @param subProductId The sub-product ID
     * @return The sub-product response
     */
    public SubProductResponseDTO getSubProduct(Integer subProductId) {
        // Find the sub-product
        SubProdMaster subProduct = subProdMasterRepository.findById(subProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", subProductId));

        // Return the response
        return mapToResponse(subProduct);
    }

    /**
     * Get all sub-products with pagination
     * 
     * @param pageable The pagination information
     * @return Page of sub-product responses
     */
    public Page<SubProductResponseDTO> getAllSubProducts(Pageable pageable) {
        // Get the sub-products page
        Page<SubProdMaster> subProducts = subProdMasterRepository.findAll(pageable);

        // Map to response DTOs
        return subProducts.map(this::mapToResponse);
    }

    /**
     * Verify a sub-product
     * 
     * @param subProductId The sub-product ID
     * @param verificationDTO The verification data
     * @return The verified sub-product response
     */
    @Transactional
    public SubProductResponseDTO verifySubProduct(Integer subProductId, CustomerVerificationDTO verificationDTO) {
        // Find the sub-product
        SubProdMaster subProduct = subProdMasterRepository.findById(subProductId)
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Product", "ID", subProductId));

        // Check if maker and verifier are different
        if (subProduct.getMakerId().equals(verificationDTO.getVerifierId())) {
            throw new BusinessException("Maker cannot verify their own record");
        }

        // Set verification fields
        subProduct.setVerifierId(verificationDTO.getVerifierId());
        // Replaced device-based date/time with System_Date (SystemDateService) - CBS Compliance Fix
        subProduct.setVerificationDate(systemDateService.getSystemDate());
        subProduct.setVerificationTime(LocalTime.now()); // Keep current time for audit timestamps

        // Save the verified sub-product
        SubProdMaster verifiedSubProduct = subProdMasterRepository.save(subProduct);
        log.info("Sub-Product verified with ID: {}", verifiedSubProduct.getSubProductId());

        // Return the response
        return mapToResponse(verifiedSubProduct);
    }

    /**
     * Map DTO to entity
     * 
     * @param dto The DTO
     * @param product The parent product
     * @return The entity
     */
    private SubProdMaster mapToEntity(SubProductRequestDTO dto, ProdMaster product) {
        return SubProdMaster.builder()
                .product(product)
                .subProductCode(dto.getSubProductCode())
                .subProductName(dto.getSubProductName())
                .inttCode(dto.getInttCode())
                .cumGLNum(dto.getCumGLNum())
                .extGLNum(dto.getExtGLNum())
                .interestIncrement(dto.getInterestIncrement())
                .interestReceivableExpenditureGLNum(dto.getInterestReceivableExpenditureGLNum())
                .interestIncomePayableGLNum(dto.getInterestIncomePayableGLNum())
                .subProductStatus(dto.getSubProductStatus())
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
    private SubProductResponseDTO mapToResponse(SubProdMaster entity) {
        return SubProductResponseDTO.builder()
                .subProductId(entity.getSubProductId())
                .productId(entity.getProduct().getProductId())
                .productCode(entity.getProduct().getProductCode())
                .productName(entity.getProduct().getProductName())
                .customerProductFlag(entity.getProduct().getCustomerProductFlag())
                .subProductCode(entity.getSubProductCode())
                .subProductName(entity.getSubProductName())
                .inttCode(entity.getInttCode())
                .cumGLNum(entity.getCumGLNum())
                .extGLNum(entity.getExtGLNum())
                .interestIncrement(entity.getInterestIncrement())
                .interestReceivableExpenditureGLNum(entity.getInterestReceivableExpenditureGLNum())
                .interestIncomePayableGLNum(entity.getInterestIncomePayableGLNum())
                .effectiveInterestRate(entity.getEffectiveInterestRate())
                .subProductStatus(entity.getSubProductStatus())
                .makerId(entity.getMakerId())
                .entryDate(entity.getEntryDate())
                .entryTime(entity.getEntryTime())
                .verifierId(entity.getVerifierId())
                .verificationDate(entity.getVerificationDate())
                .verificationTime(entity.getVerificationTime())
                .verified(entity.getVerifierId() != null)
                .build();
    }

    /**
     * Get customer sub-products (filtered by customer account GL numbers)
     * 
     * @param pageable The pagination information
     * @return Page of customer sub-products
     */
    public Page<SubProductResponseDTO> getCustomerSubProducts(Pageable pageable) {
        // Get all sub-products
        Page<SubProdMaster> subProducts = subProdMasterRepository.findAll(pageable);
        
        // Filter sub-products that have customer account GL numbers (2nd digit = 1)
        return subProducts.map(this::mapToResponse)
                .map(subProduct -> {
                    // Only include sub-products with customer account GL numbers
                    if (glValidationService.isCustomerAccountGL(subProduct.getCumGLNum())) {
                        return subProduct;
                    }
                    return null;
                })
                .map(subProduct -> subProduct); // This is a simplified filter - in real implementation, use repository query
    }

    /**
     * Get sub-products filtered by account type (liability/asset)
     * 
     * @param accountType The account type (LIABILITY/ASSET)
     * @param pageable The pagination information
     * @return Page of filtered sub-products
     */
    public Page<SubProductResponseDTO> getSubProductsByAccountType(String accountType, Pageable pageable) {
        // Get all sub-products
        Page<SubProdMaster> subProducts = subProdMasterRepository.findAll(pageable);
        
        // Filter sub-products by account type
        return subProducts.map(this::mapToResponse)
                .map(subProduct -> {
                    boolean isLiability = "LIABILITY".equalsIgnoreCase(accountType) && 
                                        glValidationService.isLiabilityGL(subProduct.getCumGLNum());
                    boolean isAsset = "ASSET".equalsIgnoreCase(accountType) && 
                                    glValidationService.isAssetGL(subProduct.getCumGLNum());
                    
                    if (isLiability || isAsset) {
                        return subProduct;
                    }
                    return null;
                })
                .map(subProduct -> subProduct); // This is a simplified filter - in real implementation, use repository query
    }

    /**
     * Get sub-products filtered by product ID and account type
     * 
     * @param productId The product ID
     * @param accountType The account type (LIABILITY/ASSET)
     * @param pageable The pagination information
     * @return Page of filtered sub-products
     */
    public Page<SubProductResponseDTO> getSubProductsByProductAndType(Integer productId, String accountType, Pageable pageable) {
        // Get sub-products by product ID (returns List, not Page)
        List<SubProdMaster> subProductsList = subProdMasterRepository.findByProductProductId(productId);
        
        // Filter by account type
        List<SubProductResponseDTO> filteredSubProducts = subProductsList.stream()
                .map(this::mapToResponse)
                .filter(subProduct -> {
                    boolean isLiability = "LIABILITY".equalsIgnoreCase(accountType) && 
                                        glValidationService.isLiabilityGL(subProduct.getCumGLNum());
                    boolean isAsset = "ASSET".equalsIgnoreCase(accountType) && 
                                    glValidationService.isAssetGL(subProduct.getCumGLNum());
                    
                    return isLiability || isAsset;
                })
                .collect(java.util.stream.Collectors.toList());
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredSubProducts.size());
        
        List<SubProductResponseDTO> paginatedSubProducts = filteredSubProducts.subList(start, end);
        
        // Create Page object
        return new org.springframework.data.domain.PageImpl<>(
                paginatedSubProducts,
                pageable,
                filteredSubProducts.size()
        );
    }
}
