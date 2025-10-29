package com.example.moneymarket.service;

import com.example.moneymarket.dto.InterestRateResponseDTO;
import com.example.moneymarket.entity.InterestRateMaster;
import com.example.moneymarket.exception.ResourceNotFoundException;
import com.example.moneymarket.repository.InterestRateMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterestRateService {

    private final InterestRateMasterRepository interestRateMasterRepository;

    public List<InterestRateResponseDTO> getAllInterestRates() {
        List<InterestRateMaster> interestRates = interestRateMasterRepository.findAll();
        return interestRates.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InterestRateResponseDTO getInterestRateByCode(String inttCode) {
        InterestRateMaster interestRate = interestRateMasterRepository.findById(inttCode)
                .orElseThrow(() -> new ResourceNotFoundException("Interest Rate", "Code", inttCode));
        return mapToResponse(interestRate);
    }

    private InterestRateResponseDTO mapToResponse(InterestRateMaster entity) {
        return InterestRateResponseDTO.builder()
                .inttCode(entity.getInttCode())
                .inttRate(entity.getInttRate())
                .inttEffctvDate(entity.getInttEffctvDate())
                .build();
    }
}
