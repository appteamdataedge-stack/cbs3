package com.example.moneymarket.controller;

import com.example.moneymarket.dto.InterestRateResponseDTO;
import com.example.moneymarket.service.InterestRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interest-rates")
@RequiredArgsConstructor
public class InterestRateController {

    private final InterestRateService interestRateService;

    @GetMapping
    public ResponseEntity<List<InterestRateResponseDTO>> getAllInterestRates() {
        List<InterestRateResponseDTO> interestRates = interestRateService.getAllInterestRates();
        return ResponseEntity.ok(interestRates);
    }

    @GetMapping("/{inttCode}")
    public ResponseEntity<InterestRateResponseDTO> getInterestRateByCode(@PathVariable String inttCode) {
        InterestRateResponseDTO interestRate = interestRateService.getInterestRateByCode(inttCode);
        return ResponseEntity.ok(interestRate);
    }
}
