package com.example.moneymarket.validation;

import com.example.moneymarket.dto.ProductRequestDTO;
import com.example.moneymarket.repository.GLSetupRepository;
import com.example.moneymarket.repository.ProdMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@Component
public class ProductValidator implements Validator {

    private final ProdMasterRepository prodMasterRepository;
    private final GLSetupRepository glSetupRepository;

    @Autowired
    public ProductValidator(ProdMasterRepository prodMasterRepository, GLSetupRepository glSetupRepository) {
        this.prodMasterRepository = prodMasterRepository;
        this.glSetupRepository = glSetupRepository;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return ProductRequestDTO.class.equals(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        ProductRequestDTO product = (ProductRequestDTO) target;

        // Product Code uniqueness validation removed

        // Validate GL Number exists in GL_setup and is at Layer 3
        var glSetup = glSetupRepository.findById(product.getCumGLNum());
        if (glSetup.isEmpty()) {
            errors.rejectValue("cumGLNum", "invalid.cumGLNum", 
                    "GL Number does not exist in GL setup");
        } else if (glSetup.get().getLayerId() != 3) {
            errors.rejectValue("cumGLNum", "invalid.cumGLNum.layer", 
                    "GL Number must be at Layer 3 for Product");
        }
    }
}
