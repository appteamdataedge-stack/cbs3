# 🚀 Quick Test Guide - Office Account Balance Validation Fix

## ⚡ 3-Minute Verification

### Prerequisites
1. Have at least one Asset office account (GL starting with "2")
2. Have at least one Liability office account (GL starting with "1")
3. Know the account numbers

---

## Test 1: Asset Office Account (Should SUCCEED even with zero balance)

### Setup
```
Find an Asset office account:
- GL Code: 240101001, 240102001, 250101001, etc. (starts with "2")
- Example: Account "OFF-ASSET-001"
```

### Test Transaction
```http
POST http://localhost:8082/api/transactions
Content-Type: application/json

{
  "lines": [
    {
      "accountNo": "OFF-ASSET-001",
      "drCrFlag": "D",
      "lcyAmt": 999999,
      "tranCcy": "USD",
      "fcyAmt": 999999,
      "exchangeRate": 1.0
    },
    {
      "accountNo": "ANY-OTHER-ACCOUNT",
      "drCrFlag": "C",
      "lcyAmt": 999999,
      "tranCcy": "USD",
      "fcyAmt": 999999,
      "exchangeRate": 1.0
    }
  ],
  "valueDate": "2025-10-27",
  "narration": "Test Asset Account - No Balance Validation"
}
```

### Expected Result
```
✅ HTTP 200 OK
✅ Transaction created successfully
✅ No balance validation error
✅ Asset account can go negative
```

### Log to Check
```
INFO: Office Asset Account OFF-ASSET-001 (GL: 240101001) - Skipping balance validation.
      Transaction allowed regardless of resulting balance: -999999
```

---

## Test 2: Liability Office Account (Should FAIL with zero balance)

### Setup
```
Find a Liability office account:
- GL Code: 130101001, 130102001, 110201001, etc. (starts with "1")
- Example: Account "OFF-LIAB-001"
- Ensure balance is 0 or low
```

### Test Transaction
```http
POST http://localhost:8082/api/transactions
Content-Type: application/json

{
  "lines": [
    {
      "accountNo": "OFF-LIAB-001",
      "drCrFlag": "D",
      "lcyAmt": 10000,
      "tranCcy": "USD",
      "fcyAmt": 10000,
      "exchangeRate": 1.0
    },
    {
      "accountNo": "ANY-OTHER-ACCOUNT",
      "drCrFlag": "C",
      "lcyAmt": 10000,
      "tranCcy": "USD",
      "fcyAmt": 10000,
      "exchangeRate": 1.0
    }
  ],
  "valueDate": "2025-10-27",
  "narration": "Test Liability Account - Should Fail"
}
```

### Expected Result
```
❌ HTTP 400 Bad Request
❌ Error: "Insufficient balance for Office Liability Account OFF-LIAB-001 (GL: 130101001)"
❌ Transaction NOT created
✅ Validation properly enforced
```

### Log to Check
```
WARN: Office Liability Account OFF-LIAB-001 (GL: 130101001) - Insufficient balance.
      Current: 0, Transaction: D 10000, Resulting: -10000
```

---

## Test 3: Liability Office Account (Should SUCCEED with sufficient balance)

### Setup
```
Same Liability office account: "OFF-LIAB-001"
First credit it to give it balance:
```

### Step 1: Add Balance
```http
POST http://localhost:8082/api/transactions
{
  "lines": [
    {
      "accountNo": "OFF-LIAB-001",
      "drCrFlag": "C",
      "lcyAmt": 50000,
      "tranCcy": "USD",
      "fcyAmt": 50000,
      "exchangeRate": 1.0
    },
    {
      "accountNo": "ANY-OTHER-ACCOUNT",
      "drCrFlag": "D",
      "lcyAmt": 50000,
      "tranCcy": "USD",
      "fcyAmt": 50000,
      "exchangeRate": 1.0
    }
  ],
  "valueDate": "2025-10-27",
  "narration": "Credit liability account"
}
```

### Step 2: Post Transaction
```http
POST http://localhost:8082/api/transactions/{tranId}/post
```

### Step 3: Now Test Debit with Sufficient Balance
```http
POST http://localhost:8082/api/transactions
{
  "lines": [
    {
      "accountNo": "OFF-LIAB-001",
      "drCrFlag": "D",
      "lcyAmt": 10000,
      "tranCcy": "USD",
      "fcyAmt": 10000,
      "exchangeRate": 1.0
    },
    {
      "accountNo": "ANY-OTHER-ACCOUNT",
      "drCrFlag": "C",
      "lcyAmt": 10000,
      "tranCcy": "USD",
      "fcyAmt": 10000,
      "exchangeRate": 1.0
    }
  ],
  "valueDate": "2025-10-27",
  "narration": "Test Liability Account - With Balance"
}
```

### Expected Result
```
✅ HTTP 200 OK
✅ Transaction created successfully
✅ Validation passed (sufficient balance)
✅ Resulting balance: 40,000
```

### Log to Check
```
INFO: Office Liability Account OFF-LIAB-001 (GL: 130101001) - Balance validation passed.
      Resulting balance: 40000
```

---

## Quick Verification Checklist

### ✅ Success Criteria

- [ ] Asset account transaction succeeds WITHOUT balance
- [ ] Liability account transaction FAILS WITHOUT balance
- [ ] Liability account transaction SUCCEEDS WITH balance
- [ ] Logs show "Skipping balance validation" for Asset accounts
- [ ] Logs show "Balance validation passed" for Liability accounts (with balance)
- [ ] Logs show "Insufficient balance" warning for Liability accounts (without balance)
- [ ] Error messages are clear and include GL code and account type

---

## Quick SQL Check

### Check Office Account GL Codes
```sql
-- See your office accounts and their GL codes
SELECT 
    Account_No,
    Acct_Name,
    GL_Num,
    CASE 
        WHEN GL_Num LIKE '2%' THEN 'ASSET (No Validation)'
        WHEN GL_Num LIKE '1%' THEN 'LIABILITY (Validation Required)'
        ELSE 'OTHER'
    END AS Account_Type,
    Current_Balance
FROM of_acct_master
ORDER BY GL_Num;
```

### Check Account Balances
```sql
-- Check specific account balance
SELECT 
    Account_No,
    Current_Balance,
    Available_Balance,
    Last_Updated
FROM acct_bal
WHERE Account_No = 'OFF-ASSET-001';  -- or 'OFF-LIAB-001'
```

---

## Troubleshooting

### Issue: Asset account still blocked

**Check:**
1. Is the GL code correct? (Should start with "2")
2. Check logs for "Skipping balance validation" message
3. Verify GL is classified as Asset in gl_setup

```sql
SELECT GL_Num, GL_Name, Layer_ID, Parent_GL_Num
FROM gl_setup
WHERE GL_Num = '240101001';  -- Your GL code
```

### Issue: Liability account allows negative balance

**Check:**
1. Is the GL code correct? (Should start with "1")
2. Check logs for validation messages
3. Verify account is identified as Liability

```sql
-- Check if GL is properly classified
SELECT GL_Num, GL_Name
FROM gl_setup
WHERE GL_Num LIKE '1%';
```

### Issue: No log messages appearing

**Check application logs:**
```bash
# In your application log file, search for:
grep "Office Asset Account\|Office Liability Account" application.log
```

---

## Expected Log Patterns

### ✅ Working Correctly

**Asset Account:**
```
INFO  c.e.m.s.TransactionValidationService - Office Asset Account OFF-ASSET-001 (GL: 240101001) - 
      Skipping balance validation. Transaction allowed regardless of resulting balance: -999999
```

**Liability Account (Success):**
```
INFO  c.e.m.s.TransactionValidationService - Office Liability Account OFF-LIAB-001 (GL: 130101001) - 
      Balance validation passed. Resulting balance: 40000
```

**Liability Account (Failure):**
```
WARN  c.e.m.s.TransactionValidationService - Office Liability Account OFF-LIAB-001 (GL: 130101001) - 
      Insufficient balance. Current: 0, Transaction: D 10000, Resulting: -10000
```

---

## One-Line Summary

**Asset office accounts (GL starting with "2")** → ✅ No validation, can go negative  
**Liability office accounts (GL starting with "1")** → ✅ Validation required, cannot go negative

---

## Status: ✅ READY TO TEST

**Compilation:** ✅ Success  
**Implementation:** ✅ Complete  
**Expected Behavior:** Asset accounts skip validation, Liability accounts enforce validation  

Test all three scenarios above to verify the fix is working correctly!

