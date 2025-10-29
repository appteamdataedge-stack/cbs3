# Account Balance Display Fix - Real-Time Balance Calculation

## Issue Description

**Problem:** The account detail page was displaying only the previous day's balance without reflecting current day's transactions. The "Interest Accrued" field was showing the wrong value (availableBalance instead of actual interest accrued).

**Root Cause:** 
1. The balance displayed was static from the `acct_bal` table (updated only during EOD)
2. Current day's transactions were not included in the balance calculation
3. Interest Accrued field was mapped to the wrong data source

**Impact:**
- Users saw outdated balance information
- Balance on account detail page didn't match transaction posting screen
- Interest accrued information was incorrect
- No real-time visibility of account status

## Solution Implemented

### Backend Changes

#### 1. Updated `AccountBalanceDTO` 
**File:** `moneymarket/src/main/java/com/example/moneymarket/dto/AccountBalanceDTO.java`

**Added Field:**
```java
private BigDecimal interestAccrued;  // Latest closing balance from acct_bal_accrual table
```

**Purpose:** Hold the latest interest accrued closing balance from the `acct_bal_accrual` table.

---

#### 2. Enhanced `BalanceService`
**File:** `moneymarket/src/main/java/com/example/moneymarket/service/BalanceService.java`

**Changes:**
- Added `AcctBalAccrualRepository` dependency
- Created `getLatestInterestAccrued()` method to fetch interest from `acct_bal_accrual` table
- Updated `getComputedAccountBalance()` to populate `interestAccrued` field

**New Method:**
```java
private BigDecimal getLatestInterestAccrued(String accountNo) {
    // Fetches most recent closing balance from acct_bal_accrual
    // Returns the latest tranDate record's closing balance
    // Returns 0 if no records found
}
```

**Logic:**
1. Query `acct_bal_accrual` table for the account
2. Find record with maximum `tranDate`
3. Return the `closingBal` from that record
4. Default to 0 if no records exist

---

#### 3. Updated `CustomerAccountResponseDTO`
**File:** `moneymarket/src/main/java/com/example/moneymarket/dto/CustomerAccountResponseDTO.java`

**Added Fields:**
```java
private BigDecimal computedBalance;     // Real-time computed balance (Prev Day + Credits - Debits)
private BigDecimal interestAccrued;     // Latest closing balance from acct_bal_accrual
```

---

#### 4. Enhanced `CustomerAccountService`
**File:** `moneymarket/src/main/java/com/example/moneymarket/service/CustomerAccountService.java`

**Changes:**
- Added `BalanceService` dependency
- Modified `mapToResponse()` method to fetch and populate:
  - `computedBalance` - real-time balance with today's transactions
  - `interestAccrued` - latest value from acct_bal_accrual

**Updated Logic:**
```java
private CustomerAccountResponseDTO mapToResponse(CustAcctMaster entity, AcctBal balance) {
    // Get computed balance (real-time balance with today's transactions)
    AccountBalanceDTO balanceDTO = balanceService.getComputedAccountBalance(entity.getAccountNo());

    return CustomerAccountResponseDTO.builder()
        .computedBalance(balanceDTO.getComputedBalance())  // Real-time balance
        .interestAccrued(balanceDTO.getInterestAccrued())  // From acct_bal_accrual
        // ... other fields
        .build();
}
```

---

### Frontend Changes

#### 1. Updated `CustomerAccountResponseDTO` Interface
**File:** `frontend/src/types/account.ts`

**Added Fields:**
```typescript
export interface CustomerAccountResponseDTO {
  currentBalance?: number;          // Static balance from acct_bal table
  availableBalance?: number;        // Previous day opening balance
  computedBalance?: number;         // Real-time computed balance
  interestAccrued?: number;         // Latest closing balance from acct_bal_accrual
}
```

---

#### 2. Updated `AccountDetails` Component
**File:** `frontend/src/pages/accounts/AccountDetails.tsx`

**Changes:**
1. **Balance Display:** Changed to show `computedBalance` (real-time) instead of `currentBalance` (static)
2. **Interest Accrued Display:** Changed to show `interestAccrued` instead of `availableBalance`
3. **Added Helper Text:** Clarified what each field represents
4. **Updated Close Logic:** Uses `computedBalance` for zero-balance check

**Updated JSX:**
```tsx
<Grid item xs={12} md={6}>
  <Paper variant="outlined" sx={{ p: 2 }}>
    <Typography variant="subtitle2" color="text.secondary">
      Balance (Real-time)
    </Typography>
    <Typography variant="h4">
      {formatAmount(account.computedBalance || account.currentBalance || 0)}
    </Typography>
    <Typography variant="caption" color="text.secondary">
      Includes today's transactions
    </Typography>
  </Paper>
</Grid>

<Grid item xs={12} md={6}>
  <Paper variant="outlined" sx={{ p: 2 }}>
    <Typography variant="subtitle2" color="text.secondary">
      Interest Accrued
    </Typography>
    <Typography variant="h4">
      {formatAmount(account.interestAccrued || 0)}
    </Typography>
    <Typography variant="caption" color="text.secondary">
      Accumulated interest balance
    </Typography>
  </Paper>
</Grid>
```

---

## How It Works

### Real-Time Balance Calculation

**Formula:**
```
Computed Balance = Previous Day's Closing Balance + Today's Credits - Today's Debits
```

**Data Sources:**
1. **Previous Day's Closing Balance:** From `acct_bal` table (3-tier fallback logic)
2. **Today's Credits:** SUM of credit transactions from `tran_table` where `Tran_Date = System_Date`
3. **Today's Debits:** SUM of debit transactions from `tran_table` where `Tran_Date = System_Date`

**SQL Logic (Conceptual):**
```sql
SELECT 
    pb.closing_balance as previous_balance,
    COALESCE(SUM(CASE WHEN t.dr_cr_flag = 'C' THEN t.lcy_amt ELSE 0 END), 0) as today_credits,
    COALESCE(SUM(CASE WHEN t.dr_cr_flag = 'D' THEN t.lcy_amt ELSE 0 END), 0) as today_debits,
    (pb.closing_balance + 
     COALESCE(SUM(CASE WHEN t.dr_cr_flag = 'C' THEN t.lcy_amt ELSE 0 END), 0) - 
     COALESCE(SUM(CASE WHEN t.dr_cr_flag = 'D' THEN t.lcy_amt ELSE 0 END), 0)) as computed_balance
FROM acct_bal pb
LEFT JOIN tran_table t ON t.account_no = pb.account_no 
    AND t.tran_date = CURRENT_DATE
WHERE pb.account_no = ?
GROUP BY pb.closing_balance;
```

### Interest Accrued Fetching

**Data Source:** `acct_bal_accrual` table

**Logic:**
1. Query all accrual records for the account
2. Filter records with non-null `tranDate`
3. Find record with maximum (latest) `tranDate`
4. Return the `closingBal` from that record
5. Default to 0 if no records found

**SQL Logic (Conceptual):**
```sql
SELECT closing_bal 
FROM acct_bal_accrual 
WHERE account_no = ? 
  AND tran_date IS NOT NULL
ORDER BY tran_date DESC 
LIMIT 1;
```

---

## Example Calculation

### Scenario
```
Account: 100000002001
Date: 2025-10-28

Previous Day's Balance (2025-10-27): 10,000.00
Today's Transactions (2025-10-28):
  - Credit 1: 2,000.00
  - Credit 2: 500.00
  - Debit 1: 1,200.00

Computation:
Previous Balance: 10,000.00
Today's Credits: 2,000.00 + 500.00 = 2,500.00
Today's Debits: 1,200.00
Computed Balance = 10,000.00 + 2,500.00 - 1,200.00 = 11,300.00
```

**Result:** Account detail page displays **11,300.00** as the balance.

---

## Benefits

✅ **Real-Time Balance** - Shows current balance including today's transactions  
✅ **Accurate Information** - Balance matches transaction posting screen  
✅ **Correct Interest Accrued** - Fetched from proper source (acct_bal_accrual)  
✅ **Better UX** - Users see up-to-date information without waiting for EOD  
✅ **Transparent Display** - Helper text clarifies what each field represents  
✅ **No Breaking Changes** - Maintains backward compatibility  

---

## Testing Scenarios

### Test Case 1: Account with No Transactions Today
**Setup:**
- Account: 100000001001
- Previous Day Balance: 5,000.00
- Today's Transactions: None

**Expected Result:**
- Balance (Real-time): 5,000.00
- Interest Accrued: [Latest from acct_bal_accrual or 0]

**Verification:**
```
Computed Balance = 5,000 + 0 - 0 = 5,000 ✓
```

---

### Test Case 2: Account with Credits Only
**Setup:**
- Account: 100000001002
- Previous Day Balance: 10,000.00
- Today's Transactions:
  - Credit: 3,000.00
  - Credit: 2,000.00

**Expected Result:**
- Balance (Real-time): 15,000.00
- Interest Accrued: [Latest from acct_bal_accrual or 0]

**Verification:**
```
Computed Balance = 10,000 + 5,000 - 0 = 15,000 ✓
```

---

### Test Case 3: Account with Debits Only
**Setup:**
- Account: 100000001003
- Previous Day Balance: 20,000.00
- Today's Transactions:
  - Debit: 5,000.00
  - Debit: 3,000.00

**Expected Result:**
- Balance (Real-time): 12,000.00
- Interest Accrued: [Latest from acct_bal_accrual or 0]

**Verification:**
```
Computed Balance = 20,000 + 0 - 8,000 = 12,000 ✓
```

---

### Test Case 4: Account with Both Credits and Debits
**Setup:**
- Account: 100000001004
- Previous Day Balance: 15,000.00
- Today's Transactions:
  - Credit: 10,000.00
  - Debit: 7,000.00
  - Credit: 5,000.00
  - Debit: 2,000.00

**Expected Result:**
- Balance (Real-time): 21,000.00
- Interest Accrued: [Latest from acct_bal_accrual or 0]

**Verification:**
```
Today's Credits = 10,000 + 5,000 = 15,000
Today's Debits = 7,000 + 2,000 = 9,000
Computed Balance = 15,000 + 15,000 - 9,000 = 21,000 ✓
```

---

### Test Case 5: Verify Interest Accrued
**Setup:**
- Account: 100000001005
- acct_bal_accrual records:
  - 2025-10-25: closing_bal = 100.00
  - 2025-10-26: closing_bal = 150.00
  - 2025-10-27: closing_bal = 200.50

**Expected Result:**
- Interest Accrued: 200.50 (latest record)

**Verification:**
```sql
SELECT closing_bal FROM acct_bal_accrual 
WHERE account_no = '100000001005'
ORDER BY tran_date DESC LIMIT 1;
-- Result: 200.50 ✓
```

---

## Verification Steps

### Backend Verification

1. **Check API Response:**
```bash
curl http://localhost:8082/api/accounts/customer/100000002001
```

**Expected Fields in Response:**
```json
{
  "accountNo": "100000002001",
  "currentBalance": 10000.00,
  "availableBalance": 10000.00,
  "computedBalance": 11300.00,
  "interestAccrued": 200.50
}
```

2. **Check Logs:**
Look for:
```
DEBUG: Computed balance for account 100000002001 on date 2025-10-28: 
       Previous Day Opening=10000.00, Current Day Debits=1200.00, 
       Current Day Credits=2500.00, Computed=11300.00
DEBUG: Latest interest accrued for account 100000002001: 200.50 (from date: 2025-10-27)
```

---

### Frontend Verification

1. **Navigate to Account Detail Page:**
```
http://localhost:5173/accounts/100000002001
```

2. **Verify Balance Display:**
- Should show "Balance (Real-time)" label
- Should display computed balance (e.g., 11,300.00)
- Should show "Includes today's transactions" caption

3. **Verify Interest Accrued Display:**
- Should show "Interest Accrued" label
- Should display value from acct_bal_accrual (e.g., 200.50)
- Should show "Accumulated interest balance" caption

4. **Test Real-Time Updates:**
- Note current balance
- Post a new transaction
- Refresh account detail page
- Verify balance reflects the new transaction

---

## Files Modified

### Backend
1. ✅ `moneymarket/src/main/java/com/example/moneymarket/dto/AccountBalanceDTO.java`
2. ✅ `moneymarket/src/main/java/com/example/moneymarket/dto/CustomerAccountResponseDTO.java`
3. ✅ `moneymarket/src/main/java/com/example/moneymarket/service/BalanceService.java`
4. ✅ `moneymarket/src/main/java/com/example/moneymarket/service/CustomerAccountService.java`

### Frontend
1. ✅ `frontend/src/types/account.ts`
2. ✅ `frontend/src/pages/accounts/AccountDetails.tsx`

---

## Compilation Status

**Backend:**
```bash
cd moneymarket
mvn clean compile -DskipTests
```
**Result:** ✅ BUILD SUCCESS

**Frontend:**
```bash
cd frontend
npm run build
```
**Result:** ✅ No linter errors

---

## Deployment Notes

1. **No Database Migration Required** - Uses existing tables and columns
2. **No Breaking Changes** - All new fields are optional
3. **Backward Compatible** - Falls back to `currentBalance` if `computedBalance` is null
4. **Immediate Effect** - Next API call returns computed balance
5. **No Configuration Changes** - Works with existing setup

---

## API Contract

### GET /api/accounts/customer/{accountNo}

**Response (Enhanced):**
```json
{
  "accountNo": "string",
  "acctName": "string",
  "currentBalance": 10000.00,
  "availableBalance": 10000.00,
  "computedBalance": 11300.00,    // NEW: Real-time balance
  "interestAccrued": 200.50,       // NEW: From acct_bal_accrual
  "accountStatus": "ACTIVE",
  ...
}
```

**New Fields:**
- `computedBalance` (number, optional): Real-time balance including today's transactions
- `interestAccrued` (number, optional): Latest closing balance from acct_bal_accrual table

---

## Known Limitations

1. **Performance:** Computes balance on every request (acceptable for single account queries)
2. **Caching:** No caching implemented (can be added if needed)
3. **Office Accounts:** Currently implemented for customer accounts only (can be extended)

---

## Future Enhancements

1. **Add Caching:** Cache computed balance with TTL of 30 seconds
2. **WebSocket Updates:** Push real-time balance updates to frontend
3. **Office Account Support:** Extend same logic to office accounts
4. **Balance History:** Add endpoint to get balance history over time
5. **Transaction Breakdown:** Show today's transactions contributing to balance

---

## Conclusion

This fix provides real-time balance visibility to users by dynamically computing the current balance including today's transactions. The "Interest Accrued" field now correctly displays the latest accumulated interest from the `acct_bal_accrual` table. Users can now see accurate, up-to-date account information that matches what they see on the transaction posting screen.

**Status:** ✅ **READY FOR TESTING AND DEPLOYMENT**

**Implementation Date:** October 28, 2025  
**Compiled:** ✅ Success (Backend & Frontend)  
**Testing:** Pending User Acceptance Testing  

For detailed testing procedures, see the "Testing Scenarios" section above.

