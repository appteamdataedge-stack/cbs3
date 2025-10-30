# Vercel Production Issue - Root Cause Analysis

## Problem
Frontend on Vercel (`https://cbs3.vercel.app/`) cannot fetch data from backend (`https://moneymarket.duckdns.org/`)

## Critical Issues Found

### Issue 1: **CONFLICTING CORS CONFIGURATIONS** 🔴 CRITICAL

**Problem**: There are TWO CORS configurations that conflict with each other:

1. **CorsConfig.java** (Line 41-42) - ✅ Includes production URLs
   ```java
   "https://cbs3.vercel.app",
   "https://moneymarket.duckdns.org"
   ```

2. **WebConfig.java** (Line 14-19) - ❌ ONLY allows localhost
   ```java
   registry.addMapping("/api/**")
       .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://localhost:4173")
   ```

**Impact**: WebConfig's CORS settings may override CorsConfig, blocking production requests!

### Issue 2: **withCredentials + CORS** 🟡 HIGH

**Problem**: Frontend uses `withCredentials: true` (line 28 in apiClient.ts)
```typescript
withCredentials: true, // Include cookies in requests
```

For cross-origin requests with credentials:
- Backend MUST explicitly allow the origin (no wildcards)
- Backend MUST set `Access-Control-Allow-Credentials: true`
- Both HTTPS endpoints must have valid SSL certificates

### Issue 3: **Missing Vercel Environment Variable** 🟡 HIGH

**Problem**: Vercel deployment likely doesn't have `VITE_API_URL` environment variable set.

**Result**: Frontend defaults to `http://localhost:8082` instead of `https://moneymarket.duckdns.org`

## Solutions

### Fix 1: Remove Conflicting WebConfig CORS (CRITICAL)

**Option A: Delete WebConfig.java** (Recommended)
Since CorsConfig.java handles all CORS, WebConfig.java is redundant and conflicting.

**Option B: Update WebConfig.java to match CorsConfig**
Make WebConfig use the same dynamic origins as CorsConfig.

### Fix 2: Verify HTTPS Configuration

Ensure both domains have valid SSL certificates:
- ✅ `https://cbs3.vercel.app` (Vercel provides SSL)
- ⚠️ `https://moneymarket.duckdns.org` (Verify SSL certificate is valid)

### Fix 3: Set Vercel Environment Variable

In Vercel dashboard:
1. Go to Project Settings → Environment Variables
2. Add: `VITE_API_URL` = `https://moneymarket.duckdns.org`
3. Redeploy

## Immediate Actions Required

1. **Remove or fix WebConfig.java** ← CRITICAL
2. **Set VITE_API_URL in Vercel** ← HIGH
3. **Verify backend SSL certificate** ← HIGH
4. **Test CORS with curl** ← MEDIUM

