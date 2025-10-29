
package com.example.moneymarket.controller;

import com.example.moneymarket.dto.GLSetupResponseDTO;
import com.example.moneymarket.service.GLSetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for GL Setup operations
 */
@RestController
@RequestMapping("/api/gl-setup")
@RequiredArgsConstructor
public class GLSetupController {

    private final GLSetupService glSetupService;

    /**
     * Get all GL setups by layer ID
     * 
     * @param layerId The layer ID
     * @return List of GL setups
     */
    @GetMapping("/layer/{layerId}")
    public ResponseEntity<List<GLSetupResponseDTO>> getGLSetupsByLayerId(@PathVariable Integer layerId) {
        List<GLSetupResponseDTO> glSetups = glSetupService.getGLSetupsByLayerId(layerId);
        return ResponseEntity.ok(glSetups);
    }

    /**
     * Get Layer 4 GL numbers for interest payable/receivable accounts
     * 
     * @return List of Layer 4 GL setups for payable/receivable accounts
     */
    @GetMapping("/interest/payable-receivable/layer4")
    public ResponseEntity<List<GLSetupResponseDTO>> getInterestPayableReceivableLayer4GLs() {
        List<GLSetupResponseDTO> layer4GLs = glSetupService.getInterestPayableReceivableLayer4GLs();
        return ResponseEntity.ok(layer4GLs);
    }

    /**
     * Get Layer 4 GL numbers for interest income/expenditure accounts
     * 
     * @return List of Layer 4 GL setups for income/expenditure accounts
     */
    @GetMapping("/interest/income-expenditure/layer4")
    public ResponseEntity<List<GLSetupResponseDTO>> getInterestIncomeExpenditureLayer4GLs() {
        List<GLSetupResponseDTO> layer4GLs = glSetupService.getInterestIncomeExpenditureLayer4GLs();
        return ResponseEntity.ok(layer4GLs);
    }
}
