package com.example.moneymarket.controller;

import com.example.moneymarket.dto.TransactionLineDTO;
import com.example.moneymarket.dto.TransactionLineResponseDTO;
import com.example.moneymarket.dto.TransactionRequestDTO;
import com.example.moneymarket.dto.TransactionResponseDTO;
import com.example.moneymarket.entity.TranTable.DrCrFlag;
import com.example.moneymarket.service.TransactionService;
import com.example.moneymarket.validation.TransactionValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@ContextConfiguration(classes = {TransactionController.class, TransactionControllerTest.TestConfig.class})
class TransactionControllerTest {
    
    @Configuration
    static class TestConfig {
        @Bean
        @Primary
        public TransactionValidator transactionValidator() {
            return new TransactionValidator(null, null) {
                @Override
                public boolean supports(Class<?> clazz) {
                    return true;
                }
                
                @Override
                public void validate(Object target, org.springframework.validation.Errors errors) {
                    // No validation
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private com.example.moneymarket.service.UnifiedAccountService unifiedAccountService;

    // Using @Primary bean from TestConfig instead of @MockBean
    // private TransactionValidator transactionValidator;

    @Autowired
    private ObjectMapper objectMapper;

    private TransactionRequestDTO transactionRequest;
    private TransactionResponseDTO transactionResponse;

    @BeforeEach
    void setUp() {
        // Setup transaction request
        TransactionLineDTO debitLine = TransactionLineDTO.builder()
                .accountNo("110101001001")
                .drCrFlag(DrCrFlag.D)
                .tranCcy("USD")
                .fcyAmt(new BigDecimal("1000.00"))
                .exchangeRate(BigDecimal.ONE)
                .lcyAmt(new BigDecimal("1000.00"))
                .build();

        TransactionLineDTO creditLine = TransactionLineDTO.builder()
                .accountNo("210101001001")
                .drCrFlag(DrCrFlag.C)
                .tranCcy("USD")
                .fcyAmt(new BigDecimal("1000.00"))
                .exchangeRate(BigDecimal.ONE)
                .lcyAmt(new BigDecimal("1000.00"))
                .build();

        transactionRequest = TransactionRequestDTO.builder()
                .valueDate(LocalDate.now())
                .narration("Test Transaction")
                .lines(Arrays.asList(debitLine, creditLine))
                .build();

        // Setup transaction response
        TransactionLineResponseDTO debitLineResponse = TransactionLineResponseDTO.builder()
                .tranId("TRN-20230101-001-1")
                .accountNo("110101001001")
                .accountName("Test Account 1")
                .drCrFlag(DrCrFlag.D)
                .tranCcy("USD")
                .fcyAmt(new BigDecimal("1000.00"))
                .exchangeRate(BigDecimal.ONE)
                .lcyAmt(new BigDecimal("1000.00"))
                .build();

        TransactionLineResponseDTO creditLineResponse = TransactionLineResponseDTO.builder()
                .tranId("TRN-20230101-001-2")
                .accountNo("210101001001")
                .accountName("Test Account 2")
                .drCrFlag(DrCrFlag.C)
                .tranCcy("USD")
                .fcyAmt(new BigDecimal("1000.00"))
                .exchangeRate(BigDecimal.ONE)
                .lcyAmt(new BigDecimal("1000.00"))
                .build();

        List<TransactionLineResponseDTO> lineResponses = Arrays.asList(debitLineResponse, creditLineResponse);

        transactionResponse = TransactionResponseDTO.builder()
                .tranId("TRN-20230101-001")
                .tranDate(LocalDate.now())
                .valueDate(LocalDate.now())
                .narration("Test Transaction")
                .lines(lineResponses)
                .balanced(true)
                .status("Entry")
                .build();
    }

    @Test
    void createTransaction_Success() throws Exception {
        // Arrange
        given(transactionService.createTransaction(any(TransactionRequestDTO.class)))
                .willReturn(transactionResponse);
        
        // The validator is already mocked with a no-op implementation in TestConfig

        // Act & Assert
        mockMvc.perform(post("/api/transactions/entry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transactionRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tranId").value("TRN-20230101-001"))
                .andExpect(jsonPath("$.balanced").value(true))
                .andExpect(jsonPath("$.lines.length()").value(2));

        verify(transactionService).createTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    void getTransaction_Success() throws Exception {
        // Arrange
        String tranId = "TRN-20230101-001";
        given(transactionService.getTransaction(tranId)).willReturn(transactionResponse);

        // Act & Assert
        mockMvc.perform(get("/api/transactions/" + tranId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tranId").value(tranId))
                .andExpect(jsonPath("$.lines.length()").value(2));

        verify(transactionService).getTransaction(tranId);
    }
}
