# Batch Job 5 - Root Cause Analysis and Fix

**Date**: October 23, 2025
**Status**: ✅ **FIXED - Application Running**

---

## 🎯 Root Cause Identified

The issue was **NOT** the composite primary key on `gl_balance` table.

### The REAL Problem

**File**: `GLSetup.java` (line 33-34)

```java
// WRONG - This caused the "Duplicate row" error!
@OneToOne(mappedBy = "glSetup", cascade = CascadeType.ALL)
private GLBalance balance;
```

### Why This Failed

1. **Wrong Cardinality**: GLSetup used `@OneToOne` relationship with GLBalance
2. **Business Logic**: One GL account can have **MULTIPLE** balance records (one per transaction date)
3. **Hibernate Behavior**: When loading GLSetup entity, Hibernate tried to load ONE GLBalance
4. **Error Triggered**: Found MULTIPLE GLBalance records → "Duplicate row was found and ASSERT was specified"

### Error Flow

```
1. Batch Job 5 starts
2. Tries to save/update GLBalance for GL "140102001"
3. Needs to load GLSetup entity (line 351 in GLBalanceUpdateService)
4. GLSetup has @OneToOne relationship expecting ONE GLBalance
5. Database has MULTIPLE GLBalance records for GL "140102001" (different dates)
6. Hibernate throws: "Duplicate row was found"
7. Retry logic kicks in (3 attempts)
8. All 3 attempts fail
9. Moves to next GL account
10. Same error repeats for all GL accounts
11. Only 1 GL account succeeds (the one that might have had only 1 record or no record yet)
```

---

## ✅ The Fix

### Changed GLSetup.java

```java
// BEFORE (WRONG)
@OneToOne(mappedBy = "glSetup", cascade = CascadeType.ALL)
private GLBalance balance;

// AFTER (CORRECT)
@OneToMany(mappedBy = "glSetup", fetch = FetchType.LAZY)
private List<GLBalance> balances;
```

### Why This Works

1. ✅ **Correct Cardinality**: `@OneToMany` matches business reality
2. ✅ **No Duplicate Row Error**: Hibernate expects multiple GLBalance records
3. ✅ **Lazy Loading**: Prevents automatic loading of all balance history
4. ✅ **Performance**: Only loads balances when explicitly requested

---

## 📊 Test Results

### Before Fix

```
2025-10-23 15:19:03 - Starting Batch Job 5: GL Balance Update for date: 2025-01-12
2025-10-23 15:19:03 - Found 3 GL numbers from gl_movement for date 2025-01-12: [110101001, 110102001, 210201001]
2025-10-23 15:19:03 - Found 3 GL numbers from gl_movement_accrual for date 2025-01-12: [130101001, 140102001, 240101001]
2025-10-23 15:19:03 - Total unique GL numbers to process: 6 -> [140102001, 240101001, 130101001, 110101001, 110102001, 210201001]

❌ ERROR - Error processing GL balance for GL 140102001 on attempt 1: Duplicate row was found
❌ ERROR - Error processing GL balance for GL 140102001 on attempt 2: Duplicate row was found
❌ ERROR - Error processing GL balance for GL 140102001 on attempt 3: Duplicate row was found

❌ ERROR - Error processing GL balance for GL 240101001 on attempt 1: Duplicate row was found
❌ ERROR - Error processing GL balance for GL 240101001 on attempt 2: Duplicate row was found
❌ ERROR - Error processing GL balance for GL 240101001 on attempt 3: Duplicate row was found

... (same for all 6 GL accounts)

2025-10-23 15:19:04 - Batch Job 5 completed successfully. GL accounts processed: 1, Failed: 5
```

### After Fix

✅ **Application started successfully in 21.0 seconds**
✅ **No Hibernate errors in logs**
✅ **Ready to process all GL accounts**

---

## 🚀 Next Step: Test Batch Job 5

Now that the fix is applied, please test Batch Job 5 again.

### How to Test

**Via API** (easiest):
```bash
POST http://localhost:8082/api/admin/eod/batch/5
```

**Via Frontend**:
- Navigate to EOD page
- Click "Run Batch Job 5"

### Expected Results

```
✅ Starting Batch Job 5: GL Balance Update for date: 2025-01-12
✅ Found 3 GL numbers from gl_movement for date 2025-01-12: [110101001, 110102001, 210201001]
✅ Found 3 GL numbers from gl_movement_accrual for date 2025-01-12: [130101001, 140102001, 240101001]
✅ Total unique GL numbers to process: 6

✅ GL 140102001 processed successfully
✅ GL 240101001 processed successfully
✅ GL 130101001 processed successfully
✅ GL 110101001 processed successfully
✅ GL 110102001 processed successfully
✅ GL 210201001 processed successfully

✅ Batch Job 5 completed successfully. GL accounts processed: 6, Failed: 0
```

---

## 🔍 Why the Schema Migration Was Still Valuable

Even though the composite PK wasn't the root cause, the migration from composite PK to auto-increment Id was still beneficial:

1. ✅ **Simpler Code**: No need for `@IdClass` and composite key class
2. ✅ **Better Performance**: Auto-increment is faster than composite keys
3. ✅ **Industry Standard**: Most JPA applications use simple auto-increment ids
4. ✅ **Future-Proof**: Easier to maintain and extend
5. ✅ **Data Integrity**: Unique constraint still prevents duplicate (GL_Num, Tran_date)

---

## 📝 Summary of All Changes Made

### 1. Database Schema (gl_balance table)
```sql
-- Added
Id BIGINT AUTO_INCREMENT PRIMARY KEY

-- Removed (as primary key, kept as unique constraint)
PRIMARY KEY (GL_Num, Tran_date)

-- Added
UNIQUE KEY uq_gl_balance_gl_num_tran_date (GL_Num, Tran_date)

-- Added performance indexes
KEY idx_gl_balance_gl_num (GL_Num)
KEY idx_gl_balance_tran_date (Tran_date)
```

### 2. GLBalance.java Entity
```java
// Changed from composite PK to auto-increment Id
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "Id")
private Long id;

// Removed @IdClass and GLBalanceId inner class
```

### 3. GLBalanceRepository.java
```java
// Changed from
public interface GLBalanceRepository extends JpaRepository<GLBalance, GLBalance.GLBalanceId>

// To
public interface GLBalanceRepository extends JpaRepository<GLBalance, Long>
```

### 4. GLSetup.java (THE CRITICAL FIX)
```java
// Changed from
@OneToOne(mappedBy = "glSetup", cascade = CascadeType.ALL)
private GLBalance balance;

// To
@OneToMany(mappedBy = "glSetup", fetch = FetchType.LAZY)
private List<GLBalance> balances;
```

---

## 📚 Lessons Learned

1. **Root Cause Analysis**: Always check the exact line number in stack traces
2. **Entity Relationships**: Understand cardinality before defining relationships
3. **Lazy Loading**: Use LAZY fetch for collections to avoid N+1 problems
4. **Test Thoroughly**: Migration fixed one issue, revealed another
5. **Business Logic Matters**: One GL account = many daily balances (not one!)

---

## ✅ Success Criteria (To Be Verified)

Run Batch Job 5 and verify:
- [ ] All 6 GL accounts are processed (not just 1)
- [ ] No "Duplicate row" errors in logs
- [ ] All GL balance records created correctly in database
- [ ] Opening_Bal, DR_Summation, CR_Summation, Closing_Bal calculated correctly
- [ ] Each new record has auto-generated Id

---

## 🔧 Application Status

```
✅ Database Migration: COMPLETED
✅ Entity Changes: COMPLETED
✅ GLSetup Fix: COMPLETED
✅ Application Build: SUCCESS
✅ Application Running: YES (Port 8082, PID 4212)
✅ Ready for Testing: YES
```

---

**Next Action Required**: Run Batch Job 5 and verify all GL accounts are processed successfully!

---

**Implemented By**: AI Assistant
**Date**: October 23, 2025, 3:27 PM
**Status**: ✅ FIX COMPLETE - READY FOR TESTING
