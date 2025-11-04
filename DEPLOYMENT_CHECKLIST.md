# SOA Module - Complete Deployment Checklist

## 📋 Pre-Deployment Verification

### ✅ Frontend Status
- [x] Build completed successfully (57.26s)
- [x] No TypeScript errors
- [x] No linter errors
- [x] Dependencies installed:
  - [x] react-select
  - [x] react-datepicker
  - [x] @types/react-select
  - [x] @types/react-datepicker
- [x] Components created:
  - [x] StatementOfAccounts.tsx
  - [x] soaService.ts
  - [x] soa.types.ts
- [x] Navigation updated:
  - [x] Sidebar.tsx
  - [x] Navbar.tsx
  - [x] AppRoutes.tsx

### ⏳ Backend Status (To Be Done)
- [ ] Database table created (`txn_hist_acct`)
- [ ] Backend rebuilt with new services
- [ ] Backend deployed to AWS EC2
- [ ] Transaction history populating on verification

## 🚀 Deployment Steps

### Step 1: Deploy Frontend to Vercel ✅ READY
```bash
cd frontend
vercel --prod
```

**Expected Output:**
```
✔ Production: https://cbs3.vercel.app [57s]
```

**Verification:**
- Visit https://cbs3.vercel.app/
- Login with credentials
- Check "Statement of Accounts" appears in sidebar

---

### Step 2: Create Database Table (Backend)
```bash
# SSH into your EC2 instance
ssh your-ec2-instance

# Navigate to project directory
cd /path/to/moneymarket

# Run SQL script
mysql -u root -p moneymarketdb < create_txn_hist_acct_table.sql
```

**Verification:**
```sql
-- Check table exists
SHOW TABLES LIKE 'txn_hist_acct';

-- Check structure
DESCRIBE txn_hist_acct;

-- Check indexes
SHOW INDEX FROM txn_hist_acct;
```

**Expected:** Table exists with 5 indexes including composite (ACC_No, TRAN_DATE)

---

### Step 3: Build Backend
```bash
cd moneymarket
mvn clean package -DskipTests
```

**Expected Output:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

**Verification:**
- Check `target/moneymarket-0.0.1-SNAPSHOT.jar` exists
- File size should be ~50-70 MB

---

### Step 4: Deploy Backend to EC2
```bash
# Stop current backend
sudo systemctl stop moneymarket

# Backup current JAR
cp /opt/moneymarket/moneymarket.jar /opt/moneymarket/moneymarket.jar.backup

# Upload new JAR
scp target/moneymarket-0.0.1-SNAPSHOT.jar your-ec2:/opt/moneymarket/moneymarket.jar

# Start backend
sudo systemctl start moneymarket

# Check status
sudo systemctl status moneymarket

# Check logs
sudo journalctl -u moneymarket -f
```

**Verification:**
- Service status shows "active (running)"
- No errors in logs
- API accessible: `curl http://localhost:8082/api/soa/accounts`

---

### Step 5: Test Transaction History Population
```bash
# Create a test transaction via API or UI
# Post the transaction
# Verify the transaction
# Check database
```

**SQL Verification:**
```sql
SELECT 
    Hist_ID,
    ACC_No,
    TRAN_ID,
    TRAN_DATE,
    TRAN_TYPE,
    TRAN_AMT,
    Opening_Balance,
    BALANCE_AFTER_TRAN,
    RCRE_DATE,
    RCRE_TIME
FROM txn_hist_acct
ORDER BY RCRE_DATE DESC, RCRE_TIME DESC
LIMIT 10;
```

**Expected:** New records appear after transaction verification

---

### Step 6: End-to-End Testing

#### Test 1: Account List
1. Navigate to https://cbs3.vercel.app/statement-of-accounts
2. Check account dropdown loads
3. Verify both customer and office accounts appear
4. Test search functionality

**Expected:** ✅ Accounts load successfully

---

#### Test 2: Date Validation
1. Select an account
2. Choose dates more than 6 months apart
3. Verify error message appears

**Expected:** ✅ "Maximum 6 months date range allowed"

---

#### Test 3: SOA Generation
1. Select an account with transactions
2. Choose valid date range (≤ 6 months)
3. Click "Generate Statement"
4. Verify Excel file downloads

**Expected:** ✅ File downloads as `SOA_{accountNo}_{fromDate}_to_{toDate}.xlsx`

---

#### Test 4: Excel Content Verification
1. Open downloaded Excel file
2. Verify header section:
   - Account details
   - Period
   - Opening balance
3. Verify transaction data:
   - All transactions in date range
   - Correct debit/credit amounts
   - Running balance updates correctly
4. Verify footer section:
   - Closing balance
   - Total debits
   - Total credits

**Expected:** ✅ All data accurate and formatted professionally

---

## 🔍 Post-Deployment Verification

### Frontend Checks
- [ ] https://cbs3.vercel.app/ loads successfully
- [ ] "Statement of Accounts" visible in sidebar
- [ ] Navigation works (clicking menu item loads page)
- [ ] No console errors (F12 → Console)
- [ ] Account dropdown loads data
- [ ] Date pickers work correctly
- [ ] Form validation works
- [ ] Generate button triggers download

### Backend Checks
- [ ] Backend service running on EC2
- [ ] API endpoints accessible:
  - [ ] GET /api/soa/accounts
  - [ ] POST /api/soa/generate
  - [ ] POST /api/soa/validate-date-range
- [ ] CORS allows requests from https://cbs3.vercel.app/
- [ ] Transaction history populating on verification
- [ ] No errors in backend logs

### Database Checks
- [ ] `txn_hist_acct` table exists
- [ ] Indexes created correctly
- [ ] Foreign key constraint active
- [ ] Records populating on transaction verification
- [ ] Balances calculating correctly

### Integration Checks
- [ ] Frontend can fetch account list
- [ ] Frontend can generate SOA
- [ ] Excel file downloads successfully
- [ ] File contains correct data
- [ ] Balances match database
- [ ] No CORS errors

---

## 🐛 Troubleshooting Guide

### Issue: "Statement of Accounts" not in sidebar
**Solution:** Clear browser cache, hard refresh (Ctrl+F5)

### Issue: Account dropdown empty
**Cause:** Backend API not accessible or CORS issue  
**Solution:** 
1. Check backend logs
2. Verify CORS configuration
3. Test API directly: `curl https://moneymarket.duckdns.org/api/soa/accounts`

### Issue: "Failed to generate statement"
**Possible Causes:**
1. No transactions in date range
2. Account doesn't exist
3. Backend error

**Solution:**
1. Check backend logs
2. Verify account has transactions
3. Test API directly with curl

### Issue: Excel file empty or wrong data
**Cause:** Transaction history not populated  
**Solution:**
1. Verify `txn_hist_acct` table has data
2. Check transaction verification is working
3. Manually verify a transaction and check database

### Issue: Date validation not working
**Cause:** Frontend validation logic  
**Solution:** Check browser console for errors

---

## 📊 Success Metrics

After deployment, verify these metrics:

### Performance
- [ ] Page load time < 3 seconds
- [ ] Account dropdown loads < 1 second
- [ ] SOA generation < 5 seconds (for 6 months data)
- [ ] Excel file size reasonable (< 5 MB for typical account)

### Functionality
- [ ] 100% of accounts appear in dropdown
- [ ] 100% of transactions appear in statements
- [ ] Balance calculations 100% accurate
- [ ] Date validation works 100% of time

### User Experience
- [ ] No errors during normal usage
- [ ] Clear error messages when issues occur
- [ ] Intuitive interface
- [ ] Professional Excel output

---

## 🎯 Rollback Plan (If Needed)

### Frontend Rollback
```bash
# Vercel automatically keeps previous deployments
# Go to Vercel dashboard → Deployments → Select previous → Promote to Production
```

### Backend Rollback
```bash
# SSH into EC2
sudo systemctl stop moneymarket
cp /opt/moneymarket/moneymarket.jar.backup /opt/moneymarket/moneymarket.jar
sudo systemctl start moneymarket
```

### Database Rollback
```sql
-- If needed, drop the table (WARNING: loses all history)
DROP TABLE IF EXISTS txn_hist_acct;
```

---

## 📞 Support Contacts

**Frontend Issues:**
- Check Vercel dashboard for deployment logs
- Review browser console (F12)

**Backend Issues:**
- SSH into EC2: `ssh your-ec2-instance`
- Check logs: `sudo journalctl -u moneymarket -f`
- Check service: `sudo systemctl status moneymarket`

**Database Issues:**
- Connect to MySQL: `mysql -u root -p moneymarketdb`
- Check tables: `SHOW TABLES;`
- Check data: `SELECT * FROM txn_hist_acct LIMIT 10;`

---

## ✅ Final Checklist

Before marking deployment complete:

- [ ] Frontend deployed to Vercel
- [ ] Backend deployed to EC2
- [ ] Database table created
- [ ] All endpoints tested
- [ ] End-to-end flow tested
- [ ] No errors in logs
- [ ] Excel generation works
- [ ] Balances accurate
- [ ] User acceptance testing passed
- [ ] Documentation updated

---

## 🎉 Deployment Complete!

Once all items are checked:

1. ✅ Frontend: DEPLOYED
2. ✅ Backend: DEPLOYED
3. ✅ Database: CONFIGURED
4. ✅ Testing: PASSED
5. ✅ Documentation: COMPLETE

**Status:** 🚀 PRODUCTION READY

---

**Deployment Date:** November 2, 2025  
**Module:** Statement of Accounts (SOA)  
**Version:** 1.0.0  
**Status:** ✅ READY FOR DEPLOYMENT

