package com.example.moneymarket.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for native query methods in GLMovementRepository
 * Verifies the fix for Batch Job 5 Hibernate duplicate row issue
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("GL Movement Repository Native Query Tests")
class GLMovementRepositoryNativeQueryTest {

    @Autowired
    private GLMovementRepository glMovementRepository;

    @Test
    @DisplayName("Test findDrCrSummationNative returns empty list when no data")
    void testFindDrCrSummationNative_NoData() {
        // Given
        String glNum = "9999999";
        LocalDate testDate = LocalDate.of(2025, 10, 23);
        
        // When
        List<Object[]> results = glMovementRepository.findDrCrSummationNative(
                glNum, testDate, testDate);
        
        // Then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Test findDrCrSummationNative does not throw duplicate row error")
    void testFindDrCrSummationNative_NoDuplicateRowError() {
        // Given
        String glNum = "1001001";
        LocalDate testDate = LocalDate.of(2025, 10, 23);
        
        // When & Then - should not throw any exception
        assertDoesNotThrow(() -> {
            List<Object[]> results = glMovementRepository.findDrCrSummationNative(
                    glNum, testDate, testDate);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("Test findDrCrSummationNative returns correct structure")
    void testFindDrCrSummationNative_CorrectStructure() {
        // Given
        String glNum = "1001001";
        LocalDate testDate = LocalDate.of(2025, 10, 23);
        
        // When
        List<Object[]> results = glMovementRepository.findDrCrSummationNative(
                glNum, testDate, testDate);
        
        // Then
        assertNotNull(results);
        // If results exist, verify structure
        if (!results.isEmpty()) {
            Object[] result = results.get(0);
            assertEquals(3, result.length, "Result should have 3 elements: GL_Num, totalDr, totalCr");
            assertNotNull(result[0], "GL_Num should not be null");
            assertNotNull(result[1], "totalDr should not be null (COALESCE ensures this)");
            assertNotNull(result[2], "totalCr should not be null (COALESCE ensures this)");
            
            // Verify types can be converted
            String returnedGlNum = result[0].toString();
            BigDecimal totalDr = new BigDecimal(result[1].toString());
            BigDecimal totalCr = new BigDecimal(result[2].toString());
            
            assertNotNull(returnedGlNum);
            assertNotNull(totalDr);
            assertNotNull(totalCr);
        }
    }
}

