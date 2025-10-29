package com.example.moneymarket.validation;

import com.example.moneymarket.dto.SubProductRequestDTO;
import com.example.moneymarket.entity.GLBalance;
import com.example.moneymarket.entity.SubProdMaster;
import com.example.moneymarket.repository.GLBalanceRepository;
import com.example.moneymarket.repository.GLSetupRepository;
import com.example.moneymarket.repository.ProdMasterRepository;
import com.example.moneymarket.repository.SubProdMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class SubProductValidator implements Validator {

    private final SubProdMasterRepository subProdMasterRepository;
    private final ProdMasterRepository prodMasterRepository;
    private final GLSetupRepository glSetupRepository;
    private final GLBalanceRepository glBalanceRepository;

    @Autowired
    public SubProductValidator(SubProdMasterRepository subProdMasterRepository,
                               ProdMasterRepository prodMasterRepository,
                               GLSetupRepository glSetupRepository,
                               GLBalanceRepository glBalanceRepository) {
        this.subProdMasterRepository = subProdMasterRepository;
        this.prodMasterRepository = prodMasterRepository;
        this.glSetupRepository = glSetupRepository;
        this.glBalanceRepository = glBalanceRepository;
    }

    @Override
    public boolean supports(@NonNull Class<?> clazz) {
        return SubProductRequestDTO.class.equals(clazz);
    }

    @Override
    public void validate(@NonNull Object target, @NonNull Errors errors) {
        SubProductRequestDTO subProduct = (SubProductRequestDTO) target;

        // Sub-Product Code uniqueness validation removed

        // Check if Product ID exists
        if (!prodMasterRepository.existsById(subProduct.getProductId())) {
            errors.rejectValue("productId", "invalid.productId", 
                    "Product ID does not exist");
        }

        // Validate GL Number exists in GL_setup and is at Layer 4
        var glSetup = glSetupRepository.findById(subProduct.getCumGLNum());
        if (glSetup.isEmpty()) {
            errors.rejectValue("cumGLNum", "invalid.cumGLNum", 
                    "GL Number does not exist in GL setup");
        } else if (glSetup.get().getLayerId() != 4) {
            errors.rejectValue("cumGLNum", "invalid.cumGLNum.layer", 
                    "GL Number must be at Layer 4 for Sub-Product");
        }

        // Validate parent-child association for GL numbers
        if (!errors.hasFieldErrors("productId") && !errors.hasFieldErrors("cumGLNum")) {
            var product = prodMasterRepository.findById(subProduct.getProductId()).orElse(null);
            if (product != null) {
                var productGLNum = product.getCumGLNum();
                var glSetupOpt = glSetupRepository.findById(subProduct.getCumGLNum());
                
                if (glSetupOpt.isPresent() && !productGLNum.equals(glSetupOpt.get().getParentGLNum())) {
                    errors.rejectValue("cumGLNum", "invalid.cumGLNum.parent", 
                            "GL Number must be a child of the Product's GL Number");
                }
            }
        }

        // Note: For updates, the validation of existing sub-products 
        // should be done at the service level since the DTO doesn't have subProductId field
        // This would happen when updating via PUT /api/subproducts/{id}
        
        // Validate status changes directly (without checking existing state)
        Optional<GLBalance> glBalance = glBalanceRepository.findLatestByGlNum(subProduct.getCumGLNum());
        if (glBalance.isPresent() && glBalance.get().getCurrentBalance().compareTo(BigDecimal.ZERO) != 0 &&
            subProduct.getSubProductStatus() == SubProdMaster.SubProductStatus.Deactive) {
            errors.rejectValue("subProductStatus", "invalid.deactivation", 
                    "Cannot set Sub-Product status to Deactive while GL balance is non-zero");
        }
    }
}
