package com.example.moneymarket.repository;

import com.example.moneymarket.entity.GLSetup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GLSetupRepository extends JpaRepository<GLSetup, String> {
    
    List<GLSetup> findByLayerId(Integer layerId);
    
    List<GLSetup> findByParentGLNum(String parentGLNum);
    
    Optional<GLSetup> findByGlNumStartingWith(String glNumPrefix);
    
    @Query("SELECT g FROM GLSetup g WHERE g.layerId = ?1 AND g.layerGLNum = ?2")
    Optional<GLSetup> findByLayerIdAndLayerGLNum(Integer layerId, String layerGLNum);
    
    @Query("SELECT g FROM GLSetup g WHERE g.parentGLNum = ?1 ORDER BY g.layerGLNum")
    List<GLSetup> findChildGLsByParentGLNumOrderByLayerGLNum(String parentGLNum);
    
    List<GLSetup> findByGlName(String glName);
    
    // Validation queries for GL consistency
    long countByLayerGLNum(String layerGLNum);
    
    long countByGlNameAndParentGLNum(String glName, String parentGLNum);
    
    /**
     * Get all GL numbers that are actively used in account creation through sub-products
     * This includes:
     * 1. GLs from sub-products that have customer accounts
     * 2. GLs from sub-products that have office accounts
     * 3. Interest-related GLs from sub-products with accounts
     * 
     * @return List of GL numbers that are actively used
     */
    @Query(value = """
        SELECT DISTINCT gl.GL_Num
        FROM gl_setup gl
        WHERE gl.GL_Num IN (
            -- Get GLs from sub-products that have customer accounts
            SELECT DISTINCT sp.Cum_GL_Num
            FROM sub_prod_master sp
            INNER JOIN cust_acct_master ca ON ca.Sub_Product_Id = sp.Sub_Product_Id
            
            UNION
            
            -- Get GLs from sub-products that have office accounts
            SELECT DISTINCT sp.Cum_GL_Num
            FROM sub_prod_master sp
            INNER JOIN of_acct_master oa ON oa.Sub_Product_Id = sp.Sub_Product_Id
            
            UNION
            
            -- Get interest income/expenditure GLs from sub-products with customer accounts
            SELECT DISTINCT sp.interest_income_expenditure_gl_num
            FROM sub_prod_master sp
            INNER JOIN cust_acct_master ca ON ca.Sub_Product_Id = sp.Sub_Product_Id
            WHERE sp.interest_income_expenditure_gl_num IS NOT NULL
            
            UNION
            
            -- Get interest receivable/payable GLs from sub-products with customer accounts
            SELECT DISTINCT sp.interest_receivable_payable_gl_num
            FROM sub_prod_master sp
            INNER JOIN cust_acct_master ca ON ca.Sub_Product_Id = sp.Sub_Product_Id
            WHERE sp.interest_receivable_payable_gl_num IS NOT NULL
            
            UNION
            
            -- Get interest income/expenditure GLs from sub-products with office accounts
            SELECT DISTINCT sp.interest_income_expenditure_gl_num
            FROM sub_prod_master sp
            INNER JOIN of_acct_master oa ON oa.Sub_Product_Id = sp.Sub_Product_Id
            WHERE sp.interest_income_expenditure_gl_num IS NOT NULL
            
            UNION
            
            -- Get interest receivable/payable GLs from sub-products with office accounts
            SELECT DISTINCT sp.interest_receivable_payable_gl_num
            FROM sub_prod_master sp
            INNER JOIN of_acct_master oa ON oa.Sub_Product_Id = sp.Sub_Product_Id
            WHERE sp.interest_receivable_payable_gl_num IS NOT NULL
        )
        ORDER BY gl.GL_Num
        """, nativeQuery = true)
    List<String> findActiveGLNumbersWithAccounts();

    /**
     * Get GL numbers for Balance Sheet only (Assets and Liabilities, excluding Income and Expenditure)
     * This filters to include:
     * 1. Main GLs (Cum_GL_Num): Liabilities (1* except Income 14*) and Assets (2* except Expenditure 24*)
     * 2. Interest-related GLs: ALL interest GLs from sub-products INCLUDING those starting with 14% or 24%
     *    These represent accrued interest balances and should appear in Balance Sheet
     *    Examples: 140103001 (IISTLTR), 240101001 (Interest Expenditure Savings Bank Regular), 240102001 (IETDCUM)
     * 3. Only GLs that are actively used in accounts (from sub-products with customer or office accounts)
     * 
     * @return List of GL numbers for Balance Sheet
     */
    @Query(value = """
        SELECT DISTINCT gl.GL_Num
        FROM gl_setup gl
        WHERE gl.GL_Num IN (
            -- Main GLs (Cum_GL_Num) from sub-products with customer accounts
            -- These should exclude Income (14%) and Expenditure (24%) GLs
            SELECT DISTINCT sp.Cum_GL_Num
            FROM sub_prod_master sp
            INNER JOIN cust_acct_master ca ON ca.Sub_Product_Id = sp.Sub_Product_Id
            WHERE (sp.Cum_GL_Num LIKE '1%' OR sp.Cum_GL_Num LIKE '2%')
            AND sp.Cum_GL_Num NOT LIKE '14%'
            AND sp.Cum_GL_Num NOT LIKE '24%'
            
            UNION
            
            -- Main GLs (Cum_GL_Num) from sub-products with office accounts
            -- These should exclude Income (14%) and Expenditure (24%) GLs
            SELECT DISTINCT sp.Cum_GL_Num
            FROM sub_prod_master sp
            INNER JOIN of_acct_master oa ON oa.Sub_Product_Id = sp.Sub_Product_Id
            WHERE (sp.Cum_GL_Num LIKE '1%' OR sp.Cum_GL_Num LIKE '2%')
            AND sp.Cum_GL_Num NOT LIKE '14%'
            AND sp.Cum_GL_Num NOT LIKE '24%'
            
            UNION
            
            -- Interest income/expenditure GLs from sub-products with customer accounts
            -- Authority: Include ALL interest GLs even if they start with 14% or 24%
            SELECT DISTINCT sp.interest_income_expenditure_gl_num
            FROM sub_prod_master sp
            INNER JOIN cust_acct_master ca ON ca.Sub_Product_Id = sp.Sub_Product_Id
            WHERE sp.interest_income_expenditure_gl_num IS NOT NULL
            
            UNION
            
            -- Interest receivable/payable GLs from sub-products with customer accounts
            -- Authority: Include ALL interest GLs even if they start with 14% or 24%
            SELECT DISTINCT sp.interest_receivable_payable_gl_num
            FROM sub_prod_master sp
            INNER JOIN cust_acct_master ca ON ca.Sub_Product_Id = sp.Sub_Product_Id
            WHERE sp.interest_receivable_payable_gl_num IS NOT NULL
            
            UNION
            
            -- Interest income/expenditure GLs from sub-products with office accounts
            -- Authority: Include ALL interest GLs even if they start with 14% or 24%
            SELECT DISTINCT sp.interest_income_expenditure_gl_num
            FROM sub_prod_master sp
            INNER JOIN of_acct_master oa ON oa.Sub_Product_Id = sp.Sub_Product_Id
            WHERE sp.interest_income_expenditure_gl_num IS NOT NULL
            
            UNION
            
            -- Interest receivable/payable GLs from sub-products with office accounts
            -- Authority: Include ALL interest GLs even if they start with 14% or 24%
            SELECT DISTINCT sp.interest_receivable_payable_gl_num
            FROM sub_prod_master sp
            INNER JOIN of_acct_master oa ON oa.Sub_Product_Id = sp.Sub_Product_Id
            WHERE sp.interest_receivable_payable_gl_num IS NOT NULL
        )
        AND (gl.GL_Num LIKE '1%' OR gl.GL_Num LIKE '2%')
        ORDER BY gl.GL_Num
        """, nativeQuery = true)
    List<String> findBalanceSheetGLNumbersWithAccounts();
}
