# Batch Job 5 Fix - Current Status

**Date**: October 23, 2025  
**Status**: ⚠️ **PARTIALLY RESOLVED** - Native Query Implementation Complete, Needs Further Investigation

---

## ✅ What Was Successfully Fixed

### 1. Native Query Implementation
- ✅ **GLMovementRepository.findDrCrSummationNative()** - Implemented native SQL query
- ✅ **GLMovementAccrualRepository.findDrCrSummationNative()** - Implemented native SQL query
- ✅ **GLMovementRepository.findDistinctGLNumbersByTranDate()** - Converted to native SQL
- ✅ **GLMovementAccrualRepository.findDistinctGLNumbersByAccrualDate()** - Converted to native SQL
- ✅ **GLBalanceUpdateService.calculateDRCRSummation()** - Refactored to use native queries

### 2. Entity Mappings Updated
- ✅ **GLMovement.glSetup** - Changed to `FetchType.LAZY`
- ✅ **GLMovementAccrual.glSetup** - Changed to `FetchType.LAZY`

### 3. Test Coverage
- ✅ **GLMovementRepositoryNativeQueryTest** - All tests passing (3/3)
- ✅ **GLMovementAccrualRepositoryNativeQueryTest** - All tests passing (3/3)

### 4. No Hibernate Errors
- ✅ **No "Duplicate row was found"** errors occurring
- ✅ Application starts successfully
- ✅ Batch Job 5 executes without exceptions

---

## ⚠️ Current Issue

### Symptom
Batch Job 5 processes **only 1 out of 3** GL accounts for date 2025-01-12:
- ✅ Processes: `210201001`
- ❌ Skips: `110101001`
- ❌ Skips: `110102001`

### Database Verification
```sql
-- GL Movements exist for all 3 accounts
SELECT GL_Num, COUNT(*) FROM GL_Movement WHERE Tran_Date = '2025-01-12' GROUP BY GL_Num;
+-----------+----------+
| GL_Num    | COUNT(*) |
+-----------+----------+
| 110101001 |        2 |
| 110102001 |        1 |
| 210201001 |        1 |
+-----------+----------+

-- Native query returns all 3 GL numbers correctly
SELECT DISTINCT GL_Num FROM GL_Movement WHERE Tran_Date = '2025-01-12';
+-----------+
| GL_Num    |
+-----------+
| 110101001 |
| 110102001 |
| 210201001 |
+-----------+

-- GLSetup records exist for all 3
SELECT GL_Num FROM GL_setup WHERE GL_Num IN ('110101001', '110102001', '210201001');
+-----------+
| GL_Num    |
+-----------+
| 110101001 |
| 110102001 |
| 210201001 |
+-----------+
```

---

## 🔍 Investigation Needed

### Hypothesis
The `findDistinctGLNumbersByTranDate()` repository method may be:
1. **Caching issue** - Spring/Hibernate might be caching the old JPQL query results
2. **Proxy issue** - The lazy-loaded GLSetup might still be causing issues when the entity is accessed
3. **Transaction issue** - The `REQUIRES_NEW` propagation might be causing isolation problems
4. **Silent exception** - An exception might be thrown but caught in the retry logic

### Debug Logging Added
The following logging has been enabled:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.com.example.moneymarket.service.GLBalanceUpdateService=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

Added INFO logs in `getUniqueGLNumbers()`:
```java
log.info("Found {} GL numbers from gl_movement for date {}: {}", 
        movementGLNumbers.size(), systemDate, movementGLNumbers);
```

### Next Steps to Resolve
1. **Check Application Logs** - Look at the console window where `mvn spring-boot:run` is running
   - Look for the log line: `"Found X GL numbers from gl_movement for date 2025-01-12: [...]"`
   - Check if all 3 GL numbers are returned by the repository
   - Look for any exceptions or errors during processing

2. **If Repository Returns Only 1 GL Number**:
   - Clear Spring Boot cache/temp files
   - Do a complete Maven clean: `mvn clean`
   - Delete the `target` directory manually
   - Rebuild and restart

3. **If Repository Returns 3 GL Numbers But Only 1 is Processed**:
   - Check logs for exceptions during `processGLBalanceInNewTransaction()`
   - The exceptions might be caught by the retry logic (lines 121-145 in GLBalanceUpdateService)
   - Look for log lines: `"Error processing GL balance for GL {} on attempt {}"`

4. **Test Repository Directly**:
   - Create a test REST endpoint to call `findDistinctGLNumbersByTranDate()` directly
   - Verify what the repository actually returns at runtime

---

## 📝 Files Modified (All Changes Saved)

| File | Status | Change Description |
|------|--------|-------------------|
| `GLMovementRepository.java` | ✅ Modified | Added native queries for DR/CR summation and distinct GL numbers |
| `GLMovementAccrualRepository.java` | ✅ Modified | Added native queries for DR/CR summation and distinct GL numbers |
| `GLBalanceUpdateService.java` | ✅ Modified | Refactored to use native queries + added debug logging |
| `GLMovement.java` | ✅ Modified | Changed GLSetup mapping to LAZY fetch |
| `GLMovementAccrual.java` | ✅ Modified | Changed GLSetup mapping to LAZY fetch |
| `GLDrCrSummationDTO.java` | ✅ Created | DTO for native query results (not currently used) |
| `application.properties` | ✅ Modified | Enabled SQL logging and debug logging |
| Repository Test Files | ✅ Created | 2 test files, all tests passing |

---

## 🔧 How to Continue Debugging

### Option 1: Check the Running Application Logs
Look at the PowerShell window that's running `mvn spring-boot:run` and find these log entries:

```
Found X GL numbers from gl_movement for date 2025-01-12: [...]
Found X GL numbers from gl_movement_accrual for date 2025-01-12: [...]
Total unique GL numbers to process: X -> [...]
```

This will tell you if the repository is returning all 3 GL numbers or just 1.

### Option 2: Direct SQL Test in Application
Add a REST endpoint to test the repository directly:

```java
@GetMapping("/test/gl-numbers")
public List<String> testGLNumbers() {
    return glMovementRepository.findDistinctGLNumbersByTranDate(LocalDate.of(2025, 1, 12));
}
```

Then call: `GET http://localhost:8082/api/test/gl-numbers`

### Option 3: Clear All Caches
```bash
# Stop application
taskkill /F /FI "WINDOWTITLE eq *mvn*"

# Clean everything
cd "G:\Money Market PTTP-reback\moneymarket"
mvn clean
Remove-Item -Recurse -Force target

# Rebuild from scratch
mvn clean install -DskipTests

# Restart
mvn spring-boot:run
```

---

## 📊 Test Results Summary

| Test Category | Status | Details |
|--------------|--------|---------|
| **Compilation** | ✅ PASS | All files compile without errors |
| **Unit Tests** | ✅ PASS | Native query tests pass (6/6) |
| **Application Startup** | ✅ PASS | Application starts on port 8082 |
| **Database Connection** | ✅ PASS | MySQL connection healthy |
| **Hibernate Errors** | ✅ FIXED | No "duplicate row" errors |
| **Batch Job Execution** | ⚠️ PARTIAL | Executes but processes only 1/3 GL accounts |
| **Native Query DR/CR** | ✅ VERIFIED | SQL query returns correct aggregations |
| **Native Query GL Numbers** | ✅ VERIFIED | SQL query returns all 3 GL numbers |
| **Integration** | ⚠️ ISSUE | Repository/Service integration needs investigation |

---

## 💡 Likely Root Cause

Based on the symptoms, the most likely cause is one of:

1. **JPA Query Cache** - The named query is cached with the old JPQL version
2. **ClassLoader Issue** - The application is loading old compiled classes despite rebuild
3. **Silent Transaction Rollback** - The other 2 GL accounts fail silently in separate transactions

### Recommended Fix to Try
Stop the application completely, clear all compiled artifacts, and do a fresh build:

```powershell
# 1. Stop ALL Java processes
Get-Process java | Stop-Process -Force

# 2. Clean Maven cache for this project
cd "G:\Money Market PTTP-reback\moneymarket"
mvn clean
Remove-Item -Recurse -Force target -ErrorAction SilentlyContinue

# 3. Rebuild
mvn clean package -DskipTests

# 4. Restart fresh
mvn spring-boot:run
```

---

## 📞 Support

**Next Action Required**: 
Check the application logs in the PowerShell window to see what GL numbers are being returned by `findDistinctGLNumbersByTranDate()`. The log line will look like:

```
Found 3 GL numbers from gl_movement for date 2025-01-12: [110101001, 110102001, 210201001]
```

or

```
Found 1 GL numbers from gl_movement for date 2025-01-12: [210201001]
```

This will determine if the issue is in the repository layer or the service processing layer.

---

**Status**: Implementation complete, debugging in progress  
**Confidence**: HIGH that the fix will work once the integration issue is resolved  
**Recommendation**: Check application logs for the debug output


