# 🚨 PRODUCTION QUICK FIX - Vercel + AWS

## Problem
Dashboard and accounts not loading on:
- Frontend: `https://cbs3.vercel.app/`
- Backend: `https://moneymarket.duckdns.org/`

## Root Cause Found ✅
**WebConfig.java** was blocking all non-localhost requests!

## Fix Applied ✅
Deleted `WebConfig.java` (conflicting CORS configuration)

---

## 🚀 Deploy Now (3 Steps)

### Step 1: Deploy Backend to AWS (5 minutes)

```bash
# SSH to AWS
ssh your-aws-instance

# Pull changes
cd /path/to/moneymarket
git pull origin main

# Rebuild
mvn clean package -DskipTests

# Restart
pkill -f moneymarket
java -jar target/moneymarket-*.jar &

# Verify
curl https://moneymarket.duckdns.org/api/actuator/health
```

### Step 2: Set Vercel Environment Variable (2 minutes)

1. Go to https://vercel.com/dashboard
2. Select your project → **Settings** → **Environment Variables**
3. Add:
   - **Name**: `VITE_API_URL`
   - **Value**: `https://moneymarket.duckdns.org`
   - **Environments**: Check all (Production, Preview, Development)
4. Click **Save**

### Step 3: Redeploy Vercel (1 minute)

1. Go to **Deployments** tab
2. Click **...** on latest deployment
3. Click **Redeploy**

---

## ✅ Verify (1 minute)

1. Open https://cbs3.vercel.app/
2. Dashboard should show data
3. Accounts page should work
4. No CORS errors in console (F12)

---

## 🆘 If Still Not Working

### Quick Check 1: CORS Headers
```bash
curl -I -X OPTIONS https://moneymarket.duckdns.org/api/accounts/customer \
  -H "Origin: https://cbs3.vercel.app"
```
Should see: `Access-Control-Allow-Origin: https://cbs3.vercel.app`

### Quick Check 2: Vercel Env Variable
```bash
vercel env ls
```
Should show `VITE_API_URL`

### Quick Check 3: Backend Running
```bash
curl https://moneymarket.duckdns.org/api/actuator/health
```
Should return: `{"status":"UP"}`

---

## What Changed

### Deleted
- ❌ `WebConfig.java` (was blocking production requests)

### Kept
- ✅ `CorsConfig.java` (correctly allows production URLs)

### Required
- ⚠️ Set `VITE_API_URL` in Vercel (Step 2 above)

---

## Expected Result

✅ Dashboard loads with data  
✅ Accounts page works  
✅ No CORS errors  
✅ All API calls succeed  

**Total Time**: ~10 minutes

See `VERCEL_DEPLOYMENT_COMPLETE_FIX.md` for detailed troubleshooting.

