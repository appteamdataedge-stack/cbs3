# Quick Fix for EC2 Dashboard & Accounts Error

## Problem
Dashboard and accounts data not loading on EC2 live server.

## Root Cause
**CORS blocking**: Backend only allows localhost origins, not your EC2 IP/domain.

## Quick Fix (5 minutes)

### Step 1: Set Environment Variable on EC2
```bash
# SSH into your EC2 instance
ssh your-ec2-instance

# Set CORS origins (replace with YOUR actual IP or domain)
export CORS_ALLOWED_ORIGINS="http://YOUR_EC2_PUBLIC_IP:3000,http://YOUR_EC2_PUBLIC_IP:5173"

# Example:
# export CORS_ALLOWED_ORIGINS="http://54.123.45.67:3000,http://54.123.45.67:5173"
```

### Step 2: Restart Backend
```bash
# Find and kill the Java process
pkill -f moneymarket

# Restart with the environment variable
cd /path/to/moneymarket
java -jar target/moneymarket-*.jar &
```

### Step 3: Update Frontend API URL
```bash
# In your frontend directory on EC2
cd /path/to/frontend

# Create .env file
cat > .env << EOF
VITE_API_URL=http://YOUR_EC2_PUBLIC_IP:8082
EOF

# Rebuild frontend
npm run build

# Restart frontend server
npx serve -s dist -l 3000 &
```

### Step 4: Verify
Open browser and navigate to:
```
http://YOUR_EC2_PUBLIC_IP:3000
```

Dashboard should now load with data!

## Alternative: Modify application.properties

If you prefer not to use environment variables:

1. Edit `moneymarket/src/main/resources/application.properties`
2. Add this line:
```properties
cors.allowed.origins=http://YOUR_EC2_IP:3000,http://YOUR_EC2_IP:5173
```
3. Rebuild: `mvn clean package -DskipTests`
4. Restart backend

## What Was Fixed

### Backend Changes:
- ✅ `CorsConfig.java` - Now supports dynamic CORS origins
- ✅ `application.properties` - Added `cors.allowed.origins` property

### Configuration Needed:
- ⚠️ Set `CORS_ALLOWED_ORIGINS` environment variable
- ⚠️ Create frontend `.env` file with `VITE_API_URL`

## Troubleshooting

**Still seeing CORS error?**
- Check the exact origin in browser console error message
- Make sure it matches what you set in `CORS_ALLOWED_ORIGINS`
- Include protocol (http/https), IP/domain, and port

**Backend not starting?**
- Check logs: `tail -f logs/application.log`
- Verify MySQL is running and accessible
- Check port 8082 is not in use: `netstat -tulpn | grep 8082`

**Frontend can't connect?**
- Verify backend is running: `curl http://localhost:8082/api/actuator/health`
- Check `.env` file exists and has correct URL
- Rebuild frontend after changing `.env`

## Need More Details?
See `EC2_DEPLOYMENT_FIX.md` for comprehensive guide.

