# Office Account Transaction Validation Flow

**Visual Guide to Conditional Balance Validation**

---

## 🎯 Quick Summary

**Asset Office Accounts (GL 2*):** ✅ NO validation → Can go negative  
**Liability Office Accounts (GL 1*):** ⚠️ WITH validation → Must stay positive  
**Customer Accounts:** 🔍 Special rules → Available balance check

---

## 📊 Validation Decision Flow

```
┌─────────────────────────────────────────────────────────┐
│         Transaction Request Received                     │
│   (Account No, Dr/Cr Flag, Amount, System Date)         │
└────────────────────┬────────────────────────────────────┘
                     │
                     v
┌─────────────────────────────────────────────────────────┐
│   Get Account Info (UnifiedAccountService)              │
│   - Fetch from cust_acct_master OR of_acct_master       │
│   - Retrieve GL_Num                                      │
│   - Classify: isCustomerAccount, isAssetAccount, etc.   │
└────────────────────┬────────────────────────────────────┘
                     │
                     v
             ┌───────┴───────┐
             │ Account Type? │
             └───────┬───────┘
                     │
        ┌────────────┴────────────┐
        │                         │
        v                         v
  ┌──────────┐            ┌──────────────┐
  │ Customer │            │    Office    │
  │ Account  │            │   Account    │
  └────┬─────┘            └──────┬───────┘
       │                         │
       v                         v
┌──────────────────┐   ┌─────────────────────┐
│ Customer Account │   │  Check GL Type      │
│   Validation     │   │  (First digit of    │
│                  │   │   GL_Num)           │
│ - Check avail.   │   └──────────┬──────────┘
│   balance        │              │
│ - Exception:     │     ┌────────┴─────────┐
│   Overdraft OK   │     │                  │
│ - Debit only     │     v                  v
└──────┬───────────┘  ┌──────┐         ┌──────────┐
       │              │ "2*" │         │   "1*"   │
       │              │ASSET │         │LIABILITY │
       │              └───┬──┘         └─────┬────┘
       │                  │                  │
       │                  v                  v
       │         ┌─────────────────┐  ┌─────────────────┐
       │         │ Skip Validation │  │Apply Validation │
       │         │                 │  │                 │
       │         │ ✅ Allow trans. │  │ Check resulting │
       │         │    regardless   │  │ balance         │
       │         │    of balance   │  │                 │
       │         │                 │  │ If negative:    │
       │         │ Log: "Skipping  │  │ ❌ REJECT       │
       │         │  validation"    │  │                 │
       │         └────────┬────────┘  │ If positive:    │
       │                  │           │ ✅ ALLOW        │
       │                  │           │                 │
       │                  │           │ Log decision    │
       │                  │           └────────┬────────┘
       │                  │                    │
       └──────────────────┴────────────────────┘
                          │
                          v
              ┌───────────────────────┐
              │  Transaction Result   │
              │                       │
              │  ✅ SUCCESS: Proceed  │
              │  ❌ FAILURE: Reject   │
              │     with error msg    │
              └───────────────────────┘
```

---

## 🔍 GL Code Classification

```
┌─────────────────────────────────────────────────────────┐
│                    GL Code Pattern                       │
└─────────────────────────────────────────────────────────┘

First Digit = "1"  →  LIABILITY
    │
    ├─ Examples:
    │   • 110101001 - Savings Bank Regular
    │   • 110102001 - Term Deposit Cumulative
    │   • 130101001 - Interest Payable SB Regular
    │   • 110201001 - Term Deposit 1 Year
    │
    └─ Validation: ⚠️ REQUIRED
        ├─ Must check balance
        ├─ Cannot go negative
        └─ Reject if insufficient

First Digit = "2"  →  ASSET
    │
    ├─ Examples:
    │   • 210201001 - Overdraft Asset
    │   • 220202001 - Staff Loan
    │   • 230201001 - Margin Loan
    │   • 240101001 - Interest Expenditure SB Regular
    │
    └─ Validation: ✅ SKIPPED
        ├─ No balance check
        ├─ Can go negative
        └─ Always allow

First Digit = "3"  →  INCOME
    │
    └─ Validation: ⚠️ CONSERVATIVE (Fallback)

First Digit = "4"  →  EXPENDITURE
    │
    └─ Validation: ⚠️ CONSERVATIVE (Fallback)
```

---

## 💼 Example Scenarios

### Scenario A: Asset Office Account (No Validation)

```
Account: 921020100101
GL Code: 210201001 (Starts with "2" = ASSET)
Type: Office Account
Current Balance: 1,000.00

Transaction: DEBIT 5,000.00

┌─────────────────────────────────┐
│   Validation Check              │
├─────────────────────────────────┤
│ 1. Is Office Account? ✅ YES    │
│ 2. GL starts with "2"? ✅ YES   │
│ 3. Classification: ASSET        │
│ 4. Skip validation? ✅ YES      │
└─────────────────────────────────┘
           │
           v
┌─────────────────────────────────┐
│   Result: ✅ ALLOWED            │
├─────────────────────────────────┤
│ Resulting Balance: -4,000.00    │
│ (Negative is OK for assets)     │
│                                 │
│ Log: "Office Asset Account      │
│ 921020100101 (GL: 210201001) -  │
│ Skipping balance validation.    │
│ Transaction allowed regardless  │
│ of resulting balance: -4000.00" │
└─────────────────────────────────┘
```

---

### Scenario B: Liability Office Account - Insufficient Balance

```
Account: 913010100101
GL Code: 130101001 (Starts with "1" = LIABILITY)
Type: Office Account
Current Balance: 1,000.00

Transaction: DEBIT 5,000.00

┌─────────────────────────────────┐
│   Validation Check              │
├─────────────────────────────────┤
│ 1. Is Office Account? ✅ YES    │
│ 2. GL starts with "1"? ✅ YES   │
│ 3. Classification: LIABILITY    │
│ 4. Apply validation? ✅ YES     │
│ 5. Resulting balance: -4,000.00 │
│ 6. Is negative? ✅ YES          │
└─────────────────────────────────┘
           │
           v
┌─────────────────────────────────┐
│   Result: ❌ REJECTED           │
├─────────────────────────────────┤
│ Error Message:                  │
│ "Insufficient balance for       │
│ Office Liability Account        │
│ 913010100101 (GL: 130101001).   │
│ Available balance: 1000.00,     │
│ Required: 5000.00.              │
│ Liability accounts cannot have  │
│ negative balances."             │
│                                 │
│ Log: "Office Liability Account  │
│ 913010100101 (GL: 130101001) -  │
│ Insufficient balance."          │
└─────────────────────────────────┘
```

---

### Scenario C: Liability Office Account - Sufficient Balance

```
Account: 913010100101
GL Code: 130101001 (Starts with "1" = LIABILITY)
Type: Office Account
Current Balance: 10,000.00

Transaction: DEBIT 5,000.00

┌─────────────────────────────────┐
│   Validation Check              │
├─────────────────────────────────┤
│ 1. Is Office Account? ✅ YES    │
│ 2. GL starts with "1"? ✅ YES   │
│ 3. Classification: LIABILITY    │
│ 4. Apply validation? ✅ YES     │
│ 5. Resulting balance: 5,000.00  │
│ 6. Is negative? ❌ NO           │
└─────────────────────────────────┘
           │
           v
┌─────────────────────────────────┐
│   Result: ✅ ALLOWED            │
├─────────────────────────────────┤
│ Resulting Balance: 5,000.00     │
│ (Positive - validation passed)  │
│                                 │
│ Log: "Office Liability Account  │
│ 913010100101 (GL: 130101001) -  │
│ Balance validation passed.      │
│ Resulting balance: 5000.00"     │
└─────────────────────────────────┘
```

---

### Scenario D: Customer Account

```
Account: 100000002001
GL Code: 110102001 (Liability Customer Account)
Type: Customer Account
Available Balance: 2,000.00
Current Balance: 2,500.00

Transaction: DEBIT 1,500.00

┌─────────────────────────────────┐
│   Validation Check              │
├─────────────────────────────────┤
│ 1. Is Customer Account? ✅ YES  │
│ 2. Use customer validation      │
│ 3. Check available balance      │
│    (not current balance)        │
│ 4. Available: 2,000.00          │
│ 5. Required: 1,500.00           │
│ 6. Sufficient? ✅ YES           │
└─────────────────────────────────┘
           │
           v
┌─────────────────────────────────┐
│   Result: ✅ ALLOWED            │
├─────────────────────────────────┤
│ Available Balance After:        │
│ 2,000.00 - 1,500.00 = 500.00    │
│                                 │
│ Note: Customer accounts use     │
│ "available balance" which is    │
│ Opening_Bal + Credits - Debits  │
└─────────────────────────────────┘
```

---

## 🏗️ Architecture Components

```
┌──────────────────────────────────────────────────────────┐
│              Transaction Creation Flow                    │
└──────────────────────────────────────────────────────────┘

Frontend (React)
   └─ /transactions/new
        │
        v
   API Request: POST /api/transactions
        │
        v
┌────────────────────────────────────┐
│    TransactionController           │
└────────────┬───────────────────────┘
             │
             v
┌────────────────────────────────────┐
│    TransactionService              │
│    - validateTransactionBalance()  │
│    - createTransaction()           │
└────────────┬───────────────────────┘
             │
             v
┌────────────────────────────────────┐
│  TransactionValidationService      │
│  - validateTransaction()           │
│  - validateCustomerAccount()       │
│  - validateOfficeAccount() ⭐      │
└────────────┬───────────────────────┘
             │
      ┌──────┴──────┐
      │             │
      v             v
┌──────────┐  ┌──────────────────┐
│ Unified  │  │  GLValidation    │
│ Account  │  │  Service         │
│ Service  │  │  - isAssetGL()   │
│ - get    │  │  - isLiability   │
│   Account│  │    GL()          │
│   Info() │  └──────────────────┘
└──────────┘
```

---

## 📝 Code References

### Main Validation Method

```java
// File: TransactionValidationService.java
// Lines: 147-197

private boolean validateOfficeAccountTransaction(
    String accountNo, 
    DrCrFlag drCrFlag, 
    BigDecimal amount, 
    BigDecimal resultingBalance, 
    UnifiedAccountService.AccountInfo accountInfo
) {
    String glNum = accountInfo.getGlNum();
    
    // ASSET: Skip validation
    if (accountInfo.isAssetAccount()) {
        log.info("Office Asset Account {} (GL: {}) - Skipping validation", 
                accountNo, glNum);
        return true;
    }
    
    // LIABILITY: Apply validation
    if (accountInfo.isLiabilityAccount()) {
        if (resultingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Insufficient balance...");
        }
        return true;
    }
    
    // Fallback: Prevent negative
    if (resultingBalance.compareTo(BigDecimal.ZERO) < 0) {
        throw new BusinessException("Cannot go negative...");
    }
    
    return true;
}
```

### GL Classification

```java
// File: GLValidationService.java
// Lines: 117-136

public boolean isLiabilityGL(String glNum) {
    return glNum != null && glNum.startsWith("1");
}

public boolean isAssetGL(String glNum) {
    return glNum != null && glNum.startsWith("2");
}
```

---

## 🧪 Test Matrix

| Account Type | GL Pattern | Current Bal | Transaction | Expected | Reason |
|-------------|------------|-------------|-------------|----------|---------|
| Office | 210201001 (Asset) | 1,000 | Debit 5,000 | ✅ Allow | Asset - No validation |
| Office | 130101001 (Liability) | 1,000 | Debit 5,000 | ❌ Reject | Liability - Insufficient |
| Office | 130101001 (Liability) | 10,000 | Debit 5,000 | ✅ Allow | Liability - Sufficient |
| Office | 210201001 (Asset) | -5,000 | Debit 2,000 | ✅ Allow | Asset - Can go negative |
| Office | 130101001 (Liability) | 5,000 | Credit 2,000 | ✅ Allow | Credit always OK |
| Customer | 110102001 | Avail: 2,000 | Debit 1,500 | ✅ Allow | Within available |
| Customer | 110102001 | Avail: 1,000 | Debit 1,500 | ❌ Reject | Exceeds available |
| Customer | 210201001 (OD) | -2,000 | Debit 1,000 | ✅ Allow | Overdraft account |

---

## ✅ Summary

**Implementation Status:** ✅ **COMPLETE AND WORKING**

**Key Points:**
1. ✅ Asset Office Accounts (GL 2*) - **No validation**
2. ✅ Liability Office Accounts (GL 1*) - **Strict validation**
3. ✅ Customer Accounts - **Available balance check**
4. ✅ Comprehensive logging for audit trail
5. ✅ Clear error messages for users
6. ✅ Clean, maintainable architecture

**No Changes Needed** - System is working as specified!

---

**Document Version:** 1.0  
**Last Updated:** October 28, 2025  
**Status:** 🟢 Production Ready

