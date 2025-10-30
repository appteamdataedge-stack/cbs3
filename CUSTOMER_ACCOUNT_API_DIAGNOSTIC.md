# Customer Account API Issues - Diagnostic Guide

## APIs Affected

1. `GET /api/accounts/customer` - Get all customer accounts
2. `GET /api/accounts/customer/{accountNo}` - Get specific account
3. `POST /api/accounts/customer` - Create new account
4. `PUT /api/accounts/customer/{accountNo}` - Update account

## Root Cause Analysis

### Primary Issue: CORS Configuration (ALREADY FIXED ✅)

The **WebConfig.java** file was blocking all production requests. This has been **deleted** in the previous fix.

**Impact**: ALL customer account APIs were being blocked by CORS when accessed from:
- Frontend: `https://cbs3.vercel.app/`
- Backend: `https://moneymarket.duckdns.org/`

### Secondary Issues to Check

#### 1. Vercel Environment Variable Not Set ⚠️

**Problem**: Frontend might still be trying to connect to `localhost:8082` instead of production backend.

**Check**:
```bash
# Open browser console on https://cbs3.vercel.app/
# Should see: API Base URL: https://moneymarket.duckdns.org/api
# If you see: API Base URL: http://localhost:8082/api
# Then environment variable is not set
```

**Solution**: Set `VITE_API_URL` in Vercel (see PRODUCTION_QUICK_FIX.md)

#### 2. Backend Not Redeployed ⚠️

**Problem**: Backend on AWS still has the old WebConfig.java file.

**Check**:
```bash
ssh your-aws-instance
cd /path/to/moneymarket
find . -name "WebConfig.java"
# Should return NOTHING (file should be deleted)
```

**Solution**: Redeploy backend (see PRODUCTION_QUICK_FIX.md Step 1)

#### 3. Database Connection Issues

**Problem**: Backend can't connect to MySQL database.

**Symptoms**:
- 500 Internal Server Error
- Backend logs show: `Communications link failure` or `Access denied`

**Check**:
```bash
# On AWS instance
mysql -h localhost -u root -p moneymarketdb
# Should connect successfully
```

**Solution**:
```bash
# Check application.properties
cat src/main/resources/application.properties | grep datasource

# Verify MySQL is running
sudo systemctl status mysql

# Check MySQL logs
sudo tail -f /var/log/mysql/error.log
```

#### 4. Missing Account Balance Records

**Problem**: Accounts exist but have no balance records in `Acct_Bal` table.

**Symptoms**:
- `GET /api/accounts/customer/{accountNo}` returns 404
- Error: "Account Balance not found for Account Number"

**Check**:
```sql
-- Check if accounts have balance records
SELECT 
    cam.Account_No,
    cam.Acct_Name,
    COUNT(ab.Account_No) as balance_records
FROM Cust_Acct_Master cam
LEFT JOIN Acct_Bal ab ON cam.Account_No = ab.Account_No
GROUP BY cam.Account_No, cam.Acct_Name
HAVING balance_records = 0;
```

**Solution**:
```sql
-- Create missing balance records
INSERT INTO Acct_Bal (Account_No, Tran_date, Current_Balance, Available_Balance, Last_Updated)
SELECT 
    Account_No,
    CURDATE(),
    0,
    0,
    NOW()
FROM Cust_Acct_Master
WHERE Account_No NOT IN (SELECT DISTINCT Account_No FROM Acct_Bal);
```

#### 5. SubProduct or Customer Not Found

**Problem**: Account references non-existent customer or subproduct.

**Symptoms**:
- `POST /api/accounts/customer` returns 404
- Error: "Customer not found" or "Sub-Product not found"

**Check**:
```sql
-- Check for orphaned accounts
SELECT cam.Account_No, cam.Cust_Id, cam.Sub_Product_Id
FROM Cust_Acct_Master cam
LEFT JOIN Cust_Master cm ON cam.Cust_Id = cm.Cust_Id
LEFT JOIN Sub_Prod_Master spm ON cam.Sub_Product_Id = spm.Sub_Product_Id
WHERE cm.Cust_Id IS NULL OR spm.Sub_Product_Id IS NULL;
```

**Solution**: Fix data integrity or update account references

---

## Testing Each API Endpoint

### 1. Test GET /api/accounts/customer (List All)

**Using curl**:
```bash
curl -X GET "https://moneymarket.duckdns.org/api/accounts/customer?page=0&size=10" \
  -H "Origin: https://cbs3.vercel.app" \
  -H "Accept: application/json"
```

**Expected Response** (200 OK):
```json
{
  "content": [
    {
      "accountNo": "...",
      "acctName": "...",
      "custName": "...",
      "currentBalance": 0,
      "availableBalance": 0,
      ...
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

**Possible Errors**:
- **403 Forbidden**: CORS issue (WebConfig.java not deleted)
- **500 Internal Server Error**: Database connection issue
- **Empty array**: No accounts in database

### 2. Test GET /api/accounts/customer/{accountNo} (Get Specific)

**Using curl**:
```bash
curl -X GET "https://moneymarket.duckdns.org/api/accounts/customer/1234567890" \
  -H "Origin: https://cbs3.vercel.app" \
  -H "Accept: application/json"
```

**Expected Response** (200 OK):
```json
{
  "accountNo": "1234567890",
  "acctName": "Savings Account",
  "custName": "John Doe",
  "currentBalance": 1000.00,
  "availableBalance": 1000.00,
  ...
}
```

**Possible Errors**:
- **404 Not Found**: Account doesn't exist
- **404 Not Found** (Account Balance): No balance record for account
- **403 Forbidden**: CORS issue

### 3. Test POST /api/accounts/customer (Create Account)

**Using curl**:
```bash
curl -X POST "https://moneymarket.duckdns.org/api/accounts/customer" \
  -H "Origin: https://cbs3.vercel.app" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "custId": 1,
    "subProductId": 1,
    "acctName": "Test Account",
    "custName": "John Doe",
    "branchCode": "001",
    "accountStatus": "Active"
  }'
```

**Expected Response** (201 Created):
```json
{
  "accountNo": "1234567890",
  "acctName": "Test Account",
  "custName": "John Doe",
  "currentBalance": 0,
  "availableBalance": 0,
  "message": "Account Number 1234567890 created",
  ...
}
```

**Possible Errors**:
- **400 Bad Request**: Validation error (missing required fields)
- **404 Not Found**: Customer or SubProduct doesn't exist
- **400 Bad Request** (Business Exception): SubProduct is not active
- **403 Forbidden**: CORS issue

### 4. Test PUT /api/accounts/customer/{accountNo} (Update Account)

**Using curl**:
```bash
curl -X PUT "https://moneymarket.duckdns.org/api/accounts/customer/1234567890" \
  -H "Origin: https://cbs3.vercel.app" \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "custId": 1,
    "subProductId": 1,
    "acctName": "Updated Account Name",
    "custName": "John Doe",
    "branchCode": "001",
    "accountStatus": "Active"
  }'
```

**Expected Response** (200 OK):
```json
{
  "accountNo": "1234567890",
  "acctName": "Updated Account Name",
  "custName": "John Doe",
  "currentBalance": 1000.00,
  "availableBalance": 1000.00,
  ...
}
```

**Possible Errors**:
- **404 Not Found**: Account doesn't exist
- **400 Bad Request**: Validation error
- **400 Bad Request** (Business Exception): Cannot close account with non-zero balance
- **403 Forbidden**: CORS issue

---

## Quick Diagnostic Script

Run this on your AWS instance to check everything:

```bash
#!/bin/bash

echo "=== Customer Account API Diagnostic ==="
echo ""

echo "1. Checking if WebConfig.java exists (should be deleted)..."
if [ -f "src/main/java/com/example/moneymarket/config/WebConfig.java" ]; then
    echo "❌ PROBLEM: WebConfig.java still exists!"
else
    echo "✅ OK: WebConfig.java deleted"
fi
echo ""

echo "2. Checking if backend is running..."
if pgrep -f "moneymarket" > /dev/null; then
    echo "✅ OK: Backend is running"
else
    echo "❌ PROBLEM: Backend is not running!"
fi
echo ""

echo "3. Checking MySQL connection..."
if mysql -u root -p${MYSQL_PASSWORD} -e "USE moneymarketdb; SELECT 1;" > /dev/null 2>&1; then
    echo "✅ OK: MySQL connection successful"
else
    echo "❌ PROBLEM: Cannot connect to MySQL!"
fi
echo ""

echo "4. Checking customer accounts..."
ACCOUNT_COUNT=$(mysql -u root -p${MYSQL_PASSWORD} -D moneymarketdb -se "SELECT COUNT(*) FROM Cust_Acct_Master;")
echo "   Total customer accounts: $ACCOUNT_COUNT"
echo ""

echo "5. Checking accounts without balance records..."
MISSING_BALANCE=$(mysql -u root -p${MYSQL_PASSWORD} -D moneymarketdb -se "
    SELECT COUNT(*)
    FROM Cust_Acct_Master cam
    LEFT JOIN Acct_Bal ab ON cam.Account_No = ab.Account_No
    WHERE ab.Account_No IS NULL;
")
if [ "$MISSING_BALANCE" -eq "0" ]; then
    echo "✅ OK: All accounts have balance records"
else
    echo "⚠️  WARNING: $MISSING_BALANCE accounts missing balance records"
fi
echo ""

echo "6. Testing API endpoint..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "Origin: https://cbs3.vercel.app" \
    "https://moneymarket.duckdns.org/api/accounts/customer?page=0&size=1")

if [ "$HTTP_CODE" -eq "200" ]; then
    echo "✅ OK: API endpoint responding (HTTP $HTTP_CODE)"
elif [ "$HTTP_CODE" -eq "403" ]; then
    echo "❌ PROBLEM: CORS blocking request (HTTP $HTTP_CODE)"
else
    echo "⚠️  WARNING: Unexpected response (HTTP $HTTP_CODE)"
fi
echo ""

echo "=== Diagnostic Complete ==="
```

---

## Resolution Steps

### Step 1: Verify CORS Fix is Deployed ✅

```bash
# SSH to AWS
ssh your-aws-instance

# Check WebConfig.java is deleted
cd /path/to/moneymarket
find . -name "WebConfig.java"
# Should return nothing

# If it still exists:
git pull origin main
mvn clean package -DskipTests
pkill -f moneymarket
java -jar target/moneymarket-*.jar &
```

### Step 2: Verify Vercel Environment Variable ✅

```bash
# Check Vercel env
vercel env ls
# Should show VITE_API_URL

# If not set:
vercel env add VITE_API_URL
# Enter: https://moneymarket.duckdns.org
# Select all environments

# Redeploy
vercel --prod
```

### Step 3: Test API Directly ✅

```bash
# Test from command line
curl -X GET "https://moneymarket.duckdns.org/api/accounts/customer?page=0&size=1" \
  -H "Origin: https://cbs3.vercel.app" \
  -H "Accept: application/json"

# Should return JSON with accounts
```

### Step 4: Test from Browser ✅

1. Open `https://cbs3.vercel.app/`
2. Open Browser Console (F12)
3. Go to Accounts page
4. Check Network tab:
   - URL should be `https://moneymarket.duckdns.org/api/accounts/customer`
   - Status should be `200 OK`
   - Response should contain account data

### Step 5: Check Backend Logs ✅

```bash
# On AWS instance
tail -f logs/application.log

# Look for:
# - CORS-related errors
# - Database connection errors
# - ResourceNotFoundException
# - BusinessException
```

---

## Common Error Messages and Solutions

### Error: "Access to XMLHttpRequest... blocked by CORS policy"

**Cause**: WebConfig.java not deleted or backend not redeployed

**Solution**:
1. Verify WebConfig.java is deleted
2. Redeploy backend
3. Clear browser cache

### Error: "Cannot connect to server"

**Cause**: Frontend using wrong API URL

**Solution**:
1. Set `VITE_API_URL` in Vercel
2. Redeploy Vercel
3. Verify in browser console

### Error: "Account Balance not found for Account Number"

**Cause**: Account exists but no balance record

**Solution**:
```sql
INSERT INTO Acct_Bal (Account_No, Tran_date, Current_Balance, Available_Balance, Last_Updated)
VALUES ('ACCOUNT_NO', CURDATE(), 0, 0, NOW());
```

### Error: "Customer not found" or "Sub-Product not found"

**Cause**: Invalid foreign key reference

**Solution**:
1. Verify customer exists: `SELECT * FROM Cust_Master WHERE Cust_Id = ?`
2. Verify subproduct exists: `SELECT * FROM Sub_Prod_Master WHERE Sub_Product_Id = ?`
3. Use valid IDs when creating/updating accounts

### Error: "Sub-Product is not active"

**Cause**: Trying to create account with inactive subproduct

**Solution**:
```sql
-- Activate subproduct
UPDATE Sub_Prod_Master
SET Sub_Product_Status = 'Active'
WHERE Sub_Product_Id = ?;
```

### Error: "Cannot close account with non-zero balance"

**Cause**: Trying to close account that has balance

**Solution**:
1. Check account balance
2. Create debit transaction to zero out balance
3. Then close account

---

## Summary

### Most Likely Cause

**CORS Configuration Issue** - Already fixed by deleting WebConfig.java

### Required Actions

1. ✅ WebConfig.java deleted (already done)
2. ⚠️ **Deploy backend to AWS** (required)
3. ⚠️ **Set VITE_API_URL in Vercel** (required)
4. ⚠️ **Redeploy Vercel** (required)
5. ✅ Test APIs (after deployment)

### Expected Result

After completing steps 2-4:
- ✅ All customer account APIs work
- ✅ Dashboard loads account data
- ✅ Accounts page shows account list
- ✅ Can create/update accounts
- ✅ No CORS errors

---

## Next Steps

1. Follow **PRODUCTION_QUICK_FIX.md** for deployment steps
2. Run diagnostic script above to verify
3. Test each API endpoint
4. Check browser console for errors
5. Review backend logs if issues persist

**Status**: Code fix complete, deployment required.

