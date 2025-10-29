# 🚀 Quick Test Guide - Account Balance Display Fix

## ⚡ 3-Minute Verification

### Test 1: View Account Balance (No Transactions Today)

**Steps:**
1. Open browser: `http://localhost:5173/accounts`
2. Click on any account (e.g., `100000002001`)
3. Observe the "Balance (Real-time)" field

**Expected Result:**
```
✅ Shows balance amount
✅ Has caption: "Includes today's transactions"
✅ Balance equals previous day's closing balance (no new transactions)
```

---

### Test 2: Post Transaction and Verify Real-Time Update

**Steps:**
1. Note current balance on account detail page (e.g., 10,000.00)
2. Navigate to: `http://localhost:5173/transactions/new`
3. Create transaction:
   ```
   Line 1: Account [your account], Credit, Amount: 2,500
   Line 2: Any other account, Debit, Amount: 2,500
   ```
4. Click "Create & Post Transaction"
5. Go back to account detail page
6. Refresh page

**Expected Result:**
```
✅ Balance increased by 2,500.00
✅ New Balance = 10,000.00 + 2,500.00 = 12,500.00
✅ Shows "Includes today's transactions" caption
✅ Balance matches transaction posting screen
```

---

### Test 3: Verify Interest Accrued Field

**Steps:**
1. On account detail page, look at "Interest Accrued" field
2. Check if it shows a value or 0

**Expected Result:**
```
✅ Shows "Interest Accrued" label
✅ Shows amount (if account has accrual records) or 0
✅ Has caption: "Accumulated interest balance"
✅ NOT showing same value as "Balance (Real-time)"
```

---

## 🔍 API Verification

### Check Backend Response

**Request:**
```bash
curl http://localhost:8082/api/accounts/customer/100000002001
```

**Expected Response (Key Fields):**
```json
{
  "accountNo": "100000002001",
  "acctName": "Account Name",
  "currentBalance": 10000.00,
  "availableBalance": 10000.00,
  "computedBalance": 12500.00,  // ✅ Real-time (with today's transactions)
  "interestAccrued": 150.50,     // ✅ From acct_bal_accrual
  "accountStatus": "ACTIVE"
}
```

**Verify:**
- ✅ `computedBalance` is present
- ✅ `interestAccrued` is present
- ✅ `computedBalance` ≠ `currentBalance` (if there are transactions today)
- ✅ Values are numbers, not null

---

## 📊 Balance Calculation Verification

### Manual Calculation Test

**Get Previous Day Balance:**
```sql
SELECT closing_bal FROM acct_bal 
WHERE account_no = '100000002001' 
AND tran_date = CURRENT_DATE - INTERVAL 1 DAY;
```
Result: e.g., 10,000.00

**Get Today's Credits:**
```sql
SELECT COALESCE(SUM(lcy_amt), 0) FROM tran_table 
WHERE account_no = '100000002001' 
AND dr_cr_flag = 'C'
AND tran_date = CURRENT_DATE;
```
Result: e.g., 2,500.00

**Get Today's Debits:**
```sql
SELECT COALESCE(SUM(lcy_amt), 0) FROM tran_table 
WHERE account_no = '100000002001' 
AND dr_cr_flag = 'D'
AND tran_date = CURRENT_DATE;
```
Result: e.g., 0.00

**Calculate:**
```
Computed Balance = 10,000 + 2,500 - 0 = 12,500.00
```

**Verify on UI:**
- Balance (Real-time) should show: **12,500.00** ✅

---

## 🐛 Troubleshooting

### Issue: Balance Not Updating After Transaction

**Check:**
1. Is the transaction posted? (not just created)
2. Refresh the account detail page (F5)
3. Check transaction date matches system date
4. Check API response has `computedBalance`

**SQL Verification:**
```sql
SELECT * FROM tran_table 
WHERE account_no = '100000002001' 
AND tran_date = CURRENT_DATE
ORDER BY tran_id DESC;
```

---

### Issue: Interest Accrued Shows 0

**This is OK if:**
- Account has no interest accrual records yet
- Batch Job 6 hasn't run yet
- Account is new

**Check acct_bal_accrual table:**
```sql
SELECT * FROM acct_bal_accrual 
WHERE account_no = '100000002001'
ORDER BY tran_date DESC 
LIMIT 1;
```

If no records: **0 is correct** ✅

---

### Issue: computedBalance Not in API Response

**Check:**
1. Backend compiled successfully?
   ```bash
   cd moneymarket
   mvn clean compile
   ```

2. Backend restarted?
   ```bash
   # Restart your Spring Boot application
   ```

3. Check logs for errors:
   ```bash
   grep "ERROR" application.log
   ```

---

## ✅ Success Checklist

- [ ] Account detail page shows "Balance (Real-time)" label
- [ ] Balance includes today's transactions
- [ ] Caption "Includes today's transactions" is visible
- [ ] "Interest Accrued" field shows correct value (or 0)
- [ ] Caption "Accumulated interest balance" is visible
- [ ] Balance updates when new transaction is posted
- [ ] API response includes `computedBalance` and `interestAccrued`
- [ ] Balance matches transaction posting screen
- [ ] No console errors in browser
- [ ] No backend errors in logs

---

## 📱 Quick Visual Check

### Before Fix
```
╔════════════════════════════╗
║  Balance                   ║
║  10,000.00                 ║  ❌ Static (previous day)
╚════════════════════════════╝

╔════════════════════════════╗
║  Interest Accrued          ║
║  10,000.00                 ║  ❌ Wrong field (availableBalance)
╚════════════════════════════╝
```

### After Fix
```
╔════════════════════════════╗
║  Balance (Real-time)       ║
║  12,500.00                 ║  ✅ Includes today's transactions
║  Includes today's trans... ║
╚════════════════════════════╝

╔════════════════════════════╗
║  Interest Accrued          ║
║  150.50                    ║  ✅ Correct from acct_bal_accrual
║  Accumulated interest...   ║
╚════════════════════════════╝
```

---

## 🎯 Expected Behavior Summary

| Field | Source | Updates | Display |
|-------|--------|---------|---------|
| **Balance (Real-time)** | Computed (Prev Day + Today's Trans) | Real-time | With today's transactions ✅ |
| **Interest Accrued** | acct_bal_accrual.closing_bal | Daily (Batch Job 6) | Latest accumulated interest ✅ |

---

## 🚦 Quick Status Check

**All Green = Success!**

```
✅ Backend compiles
✅ Frontend has no errors
✅ Account detail page loads
✅ Balance shows with caption
✅ Interest Accrued shows correct value
✅ Transaction updates balance
✅ API returns new fields
```

---

## 📞 Need Help?

**Check these documents:**
- `ACCOUNT_BALANCE_DISPLAY_FIX_SUMMARY.md` - Detailed technical documentation
- Application logs for errors
- Browser console for frontend errors

**Common Issues:**
1. Backend not restarted → Restart Spring Boot app
2. Frontend cache → Hard refresh (Ctrl+Shift+R)
3. Wrong account → Use account with transactions
4. Database not updated → Run EOD batch jobs first

---

**Status:** ✅ Ready to Test  
**Time Required:** 3-5 minutes  
**Expected Outcome:** Real-time balance display working correctly

