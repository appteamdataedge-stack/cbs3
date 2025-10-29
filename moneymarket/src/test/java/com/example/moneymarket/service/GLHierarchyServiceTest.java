package com.example.moneymarket.service;

import com.example.moneymarket.entity.GLSetup;
import com.example.moneymarket.repository.GLSetupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Test class for GLHierarchyService
 */
@ExtendWith(MockitoExtension.class)
class GLHierarchyServiceTest {

    @Mock
    private GLSetupRepository glSetupRepository;

    @InjectMocks
    private GLHierarchyService glHierarchyService;

    @BeforeEach
    void setUp() {
        // Setup mock GL hierarchy data
        setupMockGLData();
    }

    private void setupMockGLData() {
        // Layer 4: OD against TD (210201001) -> Parent: 210201000
        GLSetup layer4OD = GLSetup.builder()
                .glNum("210201001")
                .layerId(4)
                .layerGLNum("001")
                .parentGLNum("210201000")
                .glName("OD against TD")
                .build();

        // Layer 3: Overdraft (210201000) -> Parent: 210200000
        GLSetup layer3OD = GLSetup.builder()
                .glNum("210201000")
                .layerId(3)
                .layerGLNum("01000")
                .parentGLNum("210200000")
                .glName("Overdraft")
                .build();

        // Layer 4: OD Interest Income (140101001) -> Parent: 140101000
        GLSetup layer4ODInterest = GLSetup.builder()
                .glNum("140101001")
                .layerId(4)
                .layerGLNum("001")
                .parentGLNum("140101000")
                .glName("OD against TD Interest Income")
                .build();

        // Layer 3: Overdraft Interest Income (140101000) -> Parent: 140100000
        GLSetup layer3ODInterest = GLSetup.builder()
                .glNum("140101000")
                .layerId(3)
                .layerGLNum("01000")
                .parentGLNum("140100000")
                .glName("Overdraft Interest Income")
                .build();

        // Layer 4: Regular Savings (110101001) -> Parent: 110101000
        GLSetup layer4Savings = GLSetup.builder()
                .glNum("110101001")
                .layerId(4)
                .layerGLNum("001")
                .parentGLNum("110101000")
                .glName("Savings Bank Regular")
                .build();

        // Layer 3: Savings Bank (110101000) -> Parent: 110100000
        GLSetup layer3Savings = GLSetup.builder()
                .glNum("110101000")
                .layerId(3)
                .layerGLNum("01000")
                .parentGLNum("110100000")
                .glName("Savings Bank")
                .build();

        // Mock repository responses
        lenient().when(glSetupRepository.findById("210201001")).thenReturn(Optional.of(layer4OD));
        lenient().when(glSetupRepository.findById("210201000")).thenReturn(Optional.of(layer3OD));
        lenient().when(glSetupRepository.findById("140101001")).thenReturn(Optional.of(layer4ODInterest));
        lenient().when(glSetupRepository.findById("140101000")).thenReturn(Optional.of(layer3ODInterest));
        lenient().when(glSetupRepository.findById("110101001")).thenReturn(Optional.of(layer4Savings));
        lenient().when(glSetupRepository.findById("110101000")).thenReturn(Optional.of(layer3Savings));
    }

    @Test
    void testGetLayer3ParentGLNum_ForLayer4OverdraftAccount() {
        // Test Layer 4 overdraft account (210201001) -> should return Layer 3 parent (210201000)
        String result = glHierarchyService.getLayer3ParentGLNum("210201001");
        assertEquals("210201000", result);
    }

    @Test
    void testGetLayer3ParentGLNum_ForLayer3OverdraftAccount() {
        // Test Layer 3 overdraft account (210201000) -> should return itself
        String result = glHierarchyService.getLayer3ParentGLNum("210201000");
        assertEquals("210201000", result);
    }

    @Test
    void testGetLayer3ParentGLNum_ForLayer4OverdraftInterestAccount() {
        // Test Layer 4 overdraft interest account (140101001) -> should return Layer 3 parent (140101000)
        String result = glHierarchyService.getLayer3ParentGLNum("140101001");
        assertEquals("140101000", result);
    }

    @Test
    void testGetLayer3ParentGLNum_ForLayer3OverdraftInterestAccount() {
        // Test Layer 3 overdraft interest account (140101000) -> should return itself
        String result = glHierarchyService.getLayer3ParentGLNum("140101000");
        assertEquals("140101000", result);
    }

    @Test
    void testGetLayer3ParentGLNum_ForNonOverdraftAccount() {
        // Test Layer 4 savings account (110101001) -> should return Layer 3 parent (110101000)
        String result = glHierarchyService.getLayer3ParentGLNum("110101001");
        assertEquals("110101000", result);
    }

    @Test
    void testIsOverdraftAccount_ForOverdraftAccount() {
        // Test Layer 4 overdraft account
        assertTrue(glHierarchyService.isOverdraftAccount("210201001"));
        
        // Test Layer 3 overdraft account
        assertTrue(glHierarchyService.isOverdraftAccount("210201000"));
        
        // Test Layer 4 overdraft interest account
        assertTrue(glHierarchyService.isOverdraftAccount("140101001"));
        
        // Test Layer 3 overdraft interest account
        assertTrue(glHierarchyService.isOverdraftAccount("140101000"));
    }

    @Test
    void testIsOverdraftAccount_ForNonOverdraftAccount() {
        // Test Layer 4 savings account
        assertFalse(glHierarchyService.isOverdraftAccount("110101001"));
        
        // Test Layer 3 savings account
        assertFalse(glHierarchyService.isOverdraftAccount("110101000"));
    }

    @Test
    void testIsOverdraftAccountType_ForOverdraftAccount() {
        // Test Layer 4 overdraft account
        assertTrue(glHierarchyService.isOverdraftAccountType("210201001"));
        
        // Test Layer 3 overdraft account
        assertTrue(glHierarchyService.isOverdraftAccountType("210201000"));
    }

    @Test
    void testIsOverdraftAccountType_ForNonOverdraftAccount() {
        // Test Layer 4 overdraft interest account (should be false - not overdraft type)
        assertFalse(glHierarchyService.isOverdraftAccountType("140101001"));
        
        // Test Layer 3 overdraft interest account (should be false - not overdraft type)
        assertFalse(glHierarchyService.isOverdraftAccountType("140101000"));
        
        // Test Layer 4 savings account
        assertFalse(glHierarchyService.isOverdraftAccountType("110101001"));
    }

    @Test
    void testIsOverdraftInterestIncomeAccount_ForOverdraftInterestAccount() {
        // Test Layer 4 overdraft interest account
        assertTrue(glHierarchyService.isOverdraftInterestIncomeAccount("140101001"));
        
        // Test Layer 3 overdraft interest account
        assertTrue(glHierarchyService.isOverdraftInterestIncomeAccount("140101000"));
    }

    @Test
    void testIsOverdraftInterestIncomeAccount_ForNonOverdraftInterestAccount() {
        // Test Layer 4 overdraft account (should be false - not interest income type)
        assertFalse(glHierarchyService.isOverdraftInterestIncomeAccount("210201001"));
        
        // Test Layer 3 overdraft account (should be false - not interest income type)
        assertFalse(glHierarchyService.isOverdraftInterestIncomeAccount("210201000"));
        
        // Test Layer 4 savings account
        assertFalse(glHierarchyService.isOverdraftInterestIncomeAccount("110101001"));
    }
}
