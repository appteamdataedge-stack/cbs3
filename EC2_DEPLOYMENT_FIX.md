# EC2 Deployment Configuration Fix

## Problem
The application was experiencing errors fetching dashboard and accounts data on the EC2 live server due to:
1. **CORS Configuration**: The backend only allowed specific localhost origins, blocking requests from the EC2 server
2. **Frontend API URL**: The frontend was hardcoded to use `localhost:8082`
3. **Missing Environment Configuration**: No support for dynamic origins based on deployment environment

## Solutions Implemented

### 1. Backend CORS Configuration (✅ Fixed)

**File Modified**: `moneymarket/src/main/java/com/example/moneymarket/config/CorsConfig.java`

**Changes**:
- Added support for dynamic CORS origins via environment variable
- Added `cors.allowed.origins` property in `application.properties`
- All three CORS beans now use the same dynamic origin list

**How it works**:
- The `getAllowedOrigins()` method reads from `cors.allowed.origins` environment variable
- Supports comma-separated list of origins
- Maintains backward compatibility with existing localhost origins

### 2. Application Properties Update (✅ Fixed)

**File Modified**: `moneymarket/src/main/resources/application.properties`

**Added**:
```properties
# CORS Configuration (for EC2 deployment, add your frontend URL)
# Example: cors.allowed.origins=http://your-ec2-ip:3000,https://your-domain.com
cors.allowed.origins=${CORS_ALLOWED_ORIGINS:}
```

## Deployment Steps for EC2

### Backend Configuration

1. **Set Environment Variables on EC2**:
   ```bash
   export CORS_ALLOWED_ORIGINS="http://YOUR_EC2_PUBLIC_IP:3000,http://YOUR_DOMAIN.com"
   export SPRING_DATASOURCE_URL="jdbc:mysql://YOUR_DB_HOST:3306/moneymarketdb"
   export SPRING_DATASOURCE_USERNAME="your_db_user"
   export SPRING_DATASOURCE_PASSWORD="your_db_password"
   ```

2. **Or add to application.properties on EC2**:
   ```properties
   cors.allowed.origins=http://YOUR_EC2_PUBLIC_IP:3000,http://YOUR_DOMAIN.com
   ```

3. **Restart the Spring Boot application**:
   ```bash
   # Stop the current process
   pkill -f moneymarket
   
   # Rebuild if needed
   cd moneymarket
   mvn clean package -DskipTests
   
   # Start with environment variables
   java -jar target/moneymarket-*.jar
   ```

### Frontend Configuration

1. **Create `.env` file in frontend directory**:
   ```bash
   cd frontend
   nano .env
   ```

2. **Add the following content**:
   ```env
   VITE_API_URL=http://YOUR_EC2_PUBLIC_IP:8082
   # Or if using domain:
   # VITE_API_URL=https://api.your-domain.com
   ```

3. **Rebuild the frontend**:
   ```bash
   npm run build
   ```

4. **Serve the frontend**:
   ```bash
   # Using serve
   npx serve -s dist -l 3000
   
   # Or using nginx (recommended for production)
   sudo cp -r dist/* /var/www/html/
   sudo systemctl restart nginx
   ```

## Example EC2 Setup

### Scenario: EC2 with Public IP `54.123.45.67`

**Backend (on EC2)**:
```bash
export CORS_ALLOWED_ORIGINS="http://54.123.45.67:3000,http://localhost:3000"
java -jar moneymarket.jar
```

**Frontend `.env` (on EC2)**:
```env
VITE_API_URL=http://54.123.45.67:8082
```

### Scenario: Using Domain Names

**Backend**:
```bash
export CORS_ALLOWED_ORIGINS="https://app.yourdomain.com,https://www.yourdomain.com"
java -jar moneymarket.jar
```

**Frontend `.env`**:
```env
VITE_API_URL=https://api.yourdomain.com
```

## Verification Steps

1. **Check Backend CORS Configuration**:
   ```bash
   curl -I http://YOUR_EC2_IP:8082/api/accounts/customer
   ```
   Look for `Access-Control-Allow-Origin` header in response

2. **Check Frontend API Connection**:
   - Open browser console (F12)
   - Navigate to Dashboard
   - Check Network tab for API calls to correct URL
   - Verify no CORS errors in console

3. **Test Dashboard Data**:
   - Dashboard should show customer, product, subproduct, and account counts
   - No "Error loading dashboard data" message

4. **Test Accounts Page**:
   - Navigate to Accounts page
   - Should see list of accounts
   - No network errors in console

## Common Issues and Solutions

### Issue 1: CORS Error Still Appears
**Solution**: 
- Verify the `CORS_ALLOWED_ORIGINS` includes the exact origin (protocol + domain + port)
- Check browser console for the exact origin being blocked
- Restart the backend after changing CORS configuration

### Issue 2: Frontend Shows "Cannot connect to server"
**Solution**:
- Verify `VITE_API_URL` in frontend `.env` file
- Rebuild frontend after changing `.env`: `npm run build`
- Check backend is running: `curl http://YOUR_EC2_IP:8082/api/actuator/health`

### Issue 3: Database Connection Error
**Solution**:
- Verify database is accessible from EC2
- Check security groups allow MySQL port (3306)
- Verify database credentials in environment variables

### Issue 4: 404 Errors on API Endpoints
**Solution**:
- Verify backend is running on port 8082
- Check application logs: `tail -f logs/application.log`
- Verify API endpoints: `/api/accounts/customer`, `/api/accounts/office`, etc.

## Security Considerations

1. **Use HTTPS in Production**:
   - Set up SSL certificates (Let's Encrypt recommended)
   - Update CORS origins to use `https://`
   - Update frontend API URL to use `https://`

2. **Restrict CORS Origins**:
   - Only add necessary origins to `CORS_ALLOWED_ORIGINS`
   - Remove localhost origins in production
   - Use specific domains instead of wildcards

3. **Environment Variables**:
   - Never commit `.env` files to git
   - Use AWS Secrets Manager or Parameter Store for sensitive data
   - Rotate database credentials regularly

## Files Modified

1. `moneymarket/src/main/java/com/example/moneymarket/config/CorsConfig.java`
   - Added dynamic CORS origin support
   - Added `getAllowedOrigins()` method
   - Updated all CORS beans to use dynamic origins

2. `moneymarket/src/main/resources/application.properties`
   - Added `cors.allowed.origins` property
   - Added documentation comments

3. `EC2_DEPLOYMENT_FIX.md` (this file)
   - Comprehensive deployment guide

## Next Steps

1. ✅ Apply the backend changes (CorsConfig.java)
2. ✅ Update application.properties
3. ⚠️ Create frontend `.env` file with correct API URL
4. ⚠️ Set `CORS_ALLOWED_ORIGINS` environment variable on EC2
5. ⚠️ Rebuild and restart both frontend and backend
6. ⚠️ Test dashboard and accounts pages

## Support

If issues persist:
1. Check backend logs for errors
2. Check browser console for CORS/network errors
3. Verify all environment variables are set correctly
4. Ensure security groups allow necessary ports (8082 for backend, 3306 for MySQL)

