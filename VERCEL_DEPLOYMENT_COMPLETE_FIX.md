# Complete Fix for Vercel + AWS Production Issue

## 🔴 CRITICAL ISSUE FOUND AND FIXED

### Problem
Dashboard and accounts data not loading on production:
- **Frontend**: `https://cbs3.vercel.app/`
- **Backend**: `https://moneymarket.duckdns.org/`

### Root Cause
**CONFLICTING CORS CONFIGURATIONS** - Two CORS configs were fighting each other:

1. ✅ `CorsConfig.java` - Correctly included production URLs
2. ❌ `WebConfig.java` - Only allowed localhost, **OVERRIDING** CorsConfig

**Result**: All requests from Vercel were being blocked by CORS!

---

## ✅ Fixes Applied

### 1. Removed Conflicting WebConfig.java ✅
**File Deleted**: `moneymarket/src/main/java/com/example/moneymarket/config/WebConfig.java`

**Why**: It was redundant and conflicting with CorsConfig.java

**Impact**: Now only CorsConfig.java handles CORS, which correctly includes:
- `https://cbs3.vercel.app` ✅
- `https://moneymarket.duckdns.org` ✅

### 2. CORS Configuration (CorsConfig.java) ✅
Already includes production URLs (lines 41-42):
```java
"https://cbs3.vercel.app",
"https://moneymarket.duckdns.org"
```

---

## 🚀 Deployment Steps

### Step 1: Deploy Backend to AWS (REQUIRED)

```bash
# SSH into your AWS EC2 instance
ssh your-aws-instance

# Navigate to backend directory
cd /path/to/moneymarket

# Pull latest changes (includes WebConfig.java deletion)
git pull origin main

# Rebuild the application
mvn clean package -DskipTests

# Stop current backend
pkill -f moneymarket

# Start backend
java -jar target/moneymarket-*.jar &

# Verify it's running
curl https://moneymarket.duckdns.org/api/actuator/health
```

**Expected Response**:
```json
{
  "status": "UP"
}
```

### Step 2: Configure Vercel Environment Variables (REQUIRED)

#### Option A: Via Vercel Dashboard (Recommended)

1. Go to https://vercel.com/dashboard
2. Select your project (`cbs3`)
3. Go to **Settings** → **Environment Variables**
4. Add new variable:
   - **Name**: `VITE_API_URL`
   - **Value**: `https://moneymarket.duckdns.org`
   - **Environments**: Select all (Production, Preview, Development)
5. Click **Save**
6. Go to **Deployments** tab
7. Click **...** on latest deployment → **Redeploy**

#### Option B: Via Vercel CLI

```bash
# Install Vercel CLI if not installed
npm i -g vercel

# Login to Vercel
vercel login

# Set environment variable
vercel env add VITE_API_URL
# When prompted, enter: https://moneymarket.duckdns.org
# Select: Production, Preview, Development

# Redeploy
vercel --prod
```

### Step 3: Verify the Fix

#### 3.1 Check Backend CORS Headers
```bash
curl -I -X OPTIONS https://moneymarket.duckdns.org/api/accounts/customer \
  -H "Origin: https://cbs3.vercel.app" \
  -H "Access-Control-Request-Method: GET"
```

**Expected Headers**:
```
Access-Control-Allow-Origin: https://cbs3.vercel.app
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD
Access-Control-Allow-Credentials: true
```

#### 3.2 Test Frontend
1. Open https://cbs3.vercel.app/
2. Open Browser Console (F12)
3. Check Console tab - should see:
   ```
   API Base URL: https://moneymarket.duckdns.org/api
   ```
4. Check Network tab - API calls should succeed (200 OK)
5. Dashboard should show data (customer count, product count, etc.)
6. Accounts page should show account list

---

## 🔍 Troubleshooting

### Issue 1: Still Getting CORS Error

**Symptom**:
```
Access to XMLHttpRequest at 'https://moneymarket.duckdns.org/api/...' 
from origin 'https://cbs3.vercel.app' has been blocked by CORS policy
```

**Solutions**:

1. **Verify backend was redeployed**:
   ```bash
   # Check if WebConfig.java still exists (it shouldn't)
   ssh your-aws-instance
   cd /path/to/moneymarket
   find . -name "WebConfig.java"
   # Should return nothing
   ```

2. **Check backend logs**:
   ```bash
   tail -f logs/application.log
   # Look for CORS-related errors
   ```

3. **Verify CORS headers**:
   ```bash
   curl -I https://moneymarket.duckdns.org/api/accounts/customer \
     -H "Origin: https://cbs3.vercel.app"
   ```

4. **Clear browser cache**:
   - Hard refresh: Ctrl+Shift+R (Windows) or Cmd+Shift+R (Mac)
   - Or clear all browser cache and cookies

### Issue 2: Frontend Still Using localhost

**Symptom**: Network tab shows requests to `http://localhost:8082`

**Solutions**:

1. **Verify Vercel environment variable**:
   ```bash
   vercel env ls
   # Should show VITE_API_URL
   ```

2. **Check if variable is set for production**:
   - Go to Vercel Dashboard → Settings → Environment Variables
   - Verify `VITE_API_URL` is checked for "Production"

3. **Redeploy after adding variable**:
   ```bash
   vercel --prod
   ```

4. **Verify in browser**:
   - Open https://cbs3.vercel.app/
   - Open Console (F12)
   - Should see: `API Base URL: https://moneymarket.duckdns.org/api`

### Issue 3: SSL Certificate Error

**Symptom**:
```
NET::ERR_CERT_AUTHORITY_INVALID
```

**Solutions**:

1. **Verify SSL certificate is valid**:
   ```bash
   curl -v https://moneymarket.duckdns.org/api/actuator/health
   # Look for SSL certificate details
   ```

2. **Check certificate expiry**:
   ```bash
   echo | openssl s_client -servername moneymarket.duckdns.org \
     -connect moneymarket.duckdns.org:443 2>/dev/null | \
     openssl x509 -noout -dates
   ```

3. **Renew Let's Encrypt certificate** (if expired):
   ```bash
   sudo certbot renew
   sudo systemctl restart nginx  # or your web server
   ```

### Issue 4: 502 Bad Gateway

**Symptom**: Backend returns 502 error

**Solutions**:

1. **Check if backend is running**:
   ```bash
   ps aux | grep moneymarket
   # Should show java process
   ```

2. **Check backend port**:
   ```bash
   netstat -tulpn | grep 8082
   # Should show LISTEN on port 8082
   ```

3. **Check nginx/reverse proxy configuration**:
   ```bash
   sudo nginx -t
   sudo systemctl status nginx
   ```

4. **Restart backend**:
   ```bash
   pkill -f moneymarket
   java -jar target/moneymarket-*.jar &
   ```

### Issue 5: Database Connection Error

**Symptom**: 500 errors on API calls, backend logs show MySQL errors

**Solutions**:

1. **Verify MySQL is running**:
   ```bash
   sudo systemctl status mysql
   ```

2. **Test database connection**:
   ```bash
   mysql -h localhost -u root -p moneymarketdb
   ```

3. **Check application.properties**:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/moneymarketdb
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

---

## 🧪 Testing Checklist

### Backend Tests
- [ ] Backend starts without errors
- [ ] Health endpoint works: `curl https://moneymarket.duckdns.org/api/actuator/health`
- [ ] CORS headers include Vercel origin
- [ ] SSL certificate is valid
- [ ] Database connection works

### Frontend Tests
- [ ] Vercel deployment successful
- [ ] Environment variable `VITE_API_URL` is set
- [ ] Console shows correct API URL
- [ ] No CORS errors in console
- [ ] Dashboard loads with data
- [ ] Accounts page loads with data
- [ ] Can create new accounts
- [ ] Transactions work

### Integration Tests
- [ ] Dashboard shows correct counts
- [ ] Customer list loads
- [ ] Product list loads
- [ ] Account details page works
- [ ] Transaction form works
- [ ] No 401/403 errors
- [ ] No network errors

---

## 📊 Before vs After

### Before Fix

```
Frontend (Vercel): https://cbs3.vercel.app
    ↓ API Request
Backend (AWS): https://moneymarket.duckdns.org
    ↓ WebConfig.java checks CORS
    ✗ Origin not in allowed list (only localhost allowed)
    ✗ Request BLOCKED
    ✗ Dashboard shows error
```

### After Fix

```
Frontend (Vercel): https://cbs3.vercel.app
    ↓ API Request (with VITE_API_URL set)
Backend (AWS): https://moneymarket.duckdns.org
    ↓ CorsConfig.java checks CORS
    ✓ Origin in allowed list
    ✓ Request ALLOWED
    ✓ Dashboard shows data
```

---

## 🔒 Security Considerations

### 1. HTTPS Only in Production
✅ Both URLs use HTTPS:
- `https://cbs3.vercel.app` (Vercel SSL)
- `https://moneymarket.duckdns.org` (Let's Encrypt)

### 2. Credentials Handling
⚠️ Frontend uses `withCredentials: true`

**Implications**:
- Cookies are sent with requests
- Backend must explicitly allow the origin (no wildcards)
- Both domains must use HTTPS

**Current Setup**: ✅ Correctly configured

### 3. CORS Origins
✅ Only specific origins allowed (no wildcards)
✅ Production URLs hardcoded in CorsConfig.java

**Recommendation**: For additional security, remove localhost origins in production:
```java
// In production, only allow production URLs
if (System.getenv("SPRING_PROFILES_ACTIVE").equals("production")) {
    return Arrays.asList(
        "https://cbs3.vercel.app",
        "https://moneymarket.duckdns.org"
    );
}
```

### 4. Environment Variables
✅ Sensitive data in environment variables:
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET` (if used)

⚠️ **Never commit** `.env` files or credentials to git

---

## 📝 Files Modified

### Deleted
1. ❌ `moneymarket/src/main/java/com/example/moneymarket/config/WebConfig.java`
   - **Reason**: Conflicting CORS configuration
   - **Impact**: Removes localhost-only CORS restriction

### Unchanged (Already Correct)
1. ✅ `moneymarket/src/main/java/com/example/moneymarket/config/CorsConfig.java`
   - Already includes production URLs
   - Dynamic origin support via environment variables

2. ✅ `frontend/src/api/apiClient.ts`
   - Reads `VITE_API_URL` from environment
   - Correctly configured for production

---

## 🎯 Summary

### What Was Wrong
1. ❌ **WebConfig.java** only allowed localhost origins
2. ❌ **WebConfig.java** was overriding **CorsConfig.java**
3. ⚠️ Vercel environment variable not set

### What Was Fixed
1. ✅ Deleted conflicting **WebConfig.java**
2. ✅ **CorsConfig.java** now handles all CORS (includes production URLs)
3. ⚠️ **Action Required**: Set `VITE_API_URL` in Vercel

### Next Steps
1. ✅ Backend changes applied (WebConfig.java deleted)
2. ⚠️ **Deploy backend to AWS** (git pull + rebuild)
3. ⚠️ **Set VITE_API_URL in Vercel** = `https://moneymarket.duckdns.org`
4. ⚠️ **Redeploy Vercel** (after setting env variable)
5. ⚠️ **Test** (follow testing checklist)

---

## 🆘 Still Having Issues?

### Quick Diagnostics

```bash
# 1. Check backend is running
curl https://moneymarket.duckdns.org/api/actuator/health

# 2. Check CORS headers
curl -I -X OPTIONS https://moneymarket.duckdns.org/api/accounts/customer \
  -H "Origin: https://cbs3.vercel.app" \
  -H "Access-Control-Request-Method: GET"

# 3. Check Vercel environment
vercel env ls

# 4. Check backend logs
ssh your-aws-instance
tail -f /path/to/logs/application.log
```

### Contact Points
- Backend logs: `/path/to/logs/application.log`
- Vercel logs: https://vercel.com/dashboard → Deployments → View Logs
- Browser console: F12 → Console tab
- Network requests: F12 → Network tab

---

## ✅ Expected Result

After completing all steps:

1. ✅ Open https://cbs3.vercel.app/
2. ✅ Dashboard loads immediately
3. ✅ Shows customer count, product count, subproduct count, account count
4. ✅ Accounts page shows list of accounts
5. ✅ No errors in browser console
6. ✅ All API calls succeed (200 OK in Network tab)

**Status**: 🟢 **READY FOR DEPLOYMENT**

The code fix is complete. Follow the deployment steps above to apply the fix to production.

