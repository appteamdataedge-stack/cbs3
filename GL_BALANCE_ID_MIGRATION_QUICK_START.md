# GL Balance Schema Migration - Quick Start Guide

## What Changed?

Changed `gl_balance` table from **composite primary key** (GL_Num, Tran_date) to **auto-increment Id** primary key.

---

## Quick Migration Steps

### 1. Backup Database (IMPORTANT!)

```sql
USE moneymarketdb;
CREATE TABLE gl_balance_backup_20251023 AS SELECT * FROM gl_balance;
SELECT COUNT(*) FROM gl_balance_backup_20251023;
```

### 2. Stop Application

Press `Ctrl+C` in the terminal running the Spring Boot app.

### 3. Run Migration Script

```sql
SOURCE G:/Money\ Market\ PTTP-reback/add_id_column_to_gl_balance.sql
```

Or open MySQL Workbench and execute: `G:\Money Market PTTP-reback\add_id_column_to_gl_balance.sql`

### 4. Verify Migration

```sql
-- Should show Id as PRIMARY KEY and unique constraint on (GL_Num, Tran_date)
SHOW CREATE TABLE gl_balance\G

-- All records should have Id values
SELECT COUNT(*) AS total, MIN(Id), MAX(Id) FROM gl_balance;
```

### 5. Rebuild Application

```powershell
cd "G:\Money Market PTTP-reback\moneymarket"
mvn clean compile
```

Look for: `BUILD SUCCESS`

### 6. Start Application

```powershell
mvn spring-boot:run
```

Wait for: `Started MoneMarketApplication in X seconds`

### 7. Test Batch Job 5

**Option A - Via API**:
```
POST http://localhost:8082/api/admin/eod/batch/5
```

**Option B - Via Frontend**:
Navigate to EOD page → Click "Run Batch Job 5"

### 8. Verify Success

Check that:
- ✅ All GL accounts are processed (not just 1)
- ✅ No errors in console
- ✅ GL_Balance table has new records with Id values

---

## If Something Goes Wrong

### Rollback Database

```sql
USE moneymarketdb;
DROP TABLE gl_balance;
CREATE TABLE gl_balance AS SELECT * FROM gl_balance_backup_20251023;
ALTER TABLE gl_balance DROP PRIMARY KEY;
ALTER TABLE gl_balance ADD PRIMARY KEY (GL_Num, Tran_date);
```

### Rollback Code

```bash
cd "G:\Money Market PTTP-reback"
git checkout HEAD -- moneymarket/src/main/java/com/example/moneymarket/entity/GLBalance.java
git checkout HEAD -- moneymarket/src/main/java/com/example/moneymarket/repository/GLBalanceRepository.java
mvn clean compile
mvn spring-boot:run
```

---

## Files Changed

1. ✅ `add_id_column_to_gl_balance.sql` (NEW) - Migration script
2. ✅ `GLBalance.java` - Removed @IdClass, added @Id Long id
3. ✅ `GLBalanceRepository.java` - Changed to JpaRepository<GLBalance, Long>
4. ✅ `GL_BALANCE_SCHEMA_CHANGE_DOCUMENTATION.md` (NEW) - Full documentation

---

## Why This Change?

**Problem**: Batch Job 5 only processing 1 GL account instead of all
**Root Cause**: Composite primary key causing JPA/Hibernate complexity
**Solution**: Simple auto-increment Id + unique constraint
**Benefit**: Cleaner code, better performance, fewer edge cases

---

## Testing Checklist

After migration:
- [ ] Application starts without errors
- [ ] Batch Job 5 processes ALL GL accounts
- [ ] No duplicate key errors
- [ ] GL balances calculated correctly
- [ ] EOD process completes successfully

---

**Questions?** Check: `GL_BALANCE_SCHEMA_CHANGE_DOCUMENTATION.md`
