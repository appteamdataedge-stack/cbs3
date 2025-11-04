#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Script to populate txn_hist_acct with correct running balance calculation
"""

import mysql.connector
from decimal import Decimal

# Database connection
config = {
    'user': 'root',
    'password': input('Enter MySQL password: '),
    'host': 'localhost',
    'database': 'moneymarketdb',
    'raise_on_warnings': True
}

try:
    conn = mysql.connector.connect(**config)
    cursor = conn.cursor(dictionary=True)
    
    print("=" * 50)
    print("Populating txn_hist_acct with correct balance logic")
    print("=" * 50)
    
    # Clear existing data
    cursor.execute("TRUNCATE TABLE txn_hist_acct")
    cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
    
    # Fetch all verified transactions
    cursor.execute("""
        SELECT 
            t.Tran_Id,
            t.Account_No,
            t.Tran_Date,
            t.Value_Date,
            t.Dr_Cr_Flag,
            t.LCY_Amt,
            t.Narration,
            t.Tran_Ccy
        FROM tran_table t
        WHERE t.Tran_Status = 'Verified'
        ORDER BY t.Account_No ASC, t.Tran_Date ASC, t.Tran_Id ASC
    """)
    
    transactions = cursor.fetchall()
    print(f"\nFound {len(transactions)} verified transactions")
    
    # Track balance per account
    account_balances = {}
    inserted_count = 0
    
    for txn in transactions:
        account_no = txn['Account_No']
        
        # Get starting balance for new account
        if account_no not in account_balances:
            cursor.execute("""
                SELECT Current_Balance
                FROM acct_bal
                WHERE Account_No = %s
                ORDER BY Tran_Date DESC
                LIMIT 1
            """, (account_no,))
            
            result = cursor.fetchone()
            account_balances[account_no] = Decimal(result['Current_Balance']) if result and result['Current_Balance'] else Decimal('0')
            print(f"Account {account_no}: Starting balance = {account_balances[account_no]}")
        
        previous_balance = account_balances[account_no]
        
        # Get branch ID
        cursor.execute("SELECT Branch_Code FROM cust_acct_master WHERE Account_No = %s LIMIT 1", (account_no,))
        branch_result = cursor.fetchone()
        branch_id = branch_result['Branch_Code'] if branch_result else 'DEFAULT'
        
        if branch_id is None:
            cursor.execute("SELECT Branch_Code FROM of_acct_master WHERE Account_No = %s LIMIT 1", (account_no,))
            branch_result = cursor.fetchone()
            branch_id = branch_result['Branch_Code'] if branch_result else 'DEFAULT'
        
        # Get GL number
        cursor.execute("""
            SELECT sp.Cum_GL_Num
            FROM cust_acct_master ca
            JOIN sub_prod_master sp ON ca.Sub_Product_Id = sp.Sub_Product_Id
            WHERE ca.Account_No = %s
            LIMIT 1
        """, (account_no,))
        gl_result = cursor.fetchone()
        gl_num = gl_result['Cum_GL_Num'] if gl_result else None
        
        if gl_num is None:
            cursor.execute("""
                SELECT sp.Cum_GL_Num
                FROM of_acct_master oa
                JOIN sub_prod_master sp ON oa.Sub_Product_Id = sp.Sub_Product_Id
                WHERE oa.Account_No = %s
                LIMIT 1
            """, (account_no,))
            gl_result = cursor.fetchone()
            gl_num = gl_result['Cum_GL_Num'] if gl_result else None
        
        # Calculate balance after transaction
        if txn['Dr_Cr_Flag'] == 'C':
            balance_after = previous_balance + txn['LCY_Amt']
        else:
            balance_after = previous_balance - txn['LCY_Amt']
        
        # Insert into txn_hist_acct
        cursor.execute("""
            INSERT INTO txn_hist_acct (
                Branch_ID, ACC_No, TRAN_ID, TRAN_DATE, VALUE_DATE, TRAN_SL_NO,
                NARRATION, TRAN_TYPE, TRAN_AMT, BALANCE_AFTER_TRAN,
                ENTRY_USER_ID, AUTH_USER_ID, CURRENCY_CODE, GL_Num, RCRE_DATE, RCRE_TIME
            ) VALUES (
                %s, %s, %s, %s, %s, 1, %s, %s, %s, %s, 'SYSTEM', 'MIGRATION', %s, %s, CURDATE(), CURTIME()
            )
        """, (
            branch_id, account_no, txn['Tran_Id'], txn['Tran_Date'], txn['Value_Date'],
            (txn['Narration'] or '')[:100], txn['Dr_Cr_Flag'], txn['LCY_Amt'], balance_after,
            txn['Tran_Ccy'] or 'BDT', gl_num
        ))
        
        # Update tracked balance
        account_balances[account_no] = balance_after
        inserted_count += 1
        
        # Print progress for sample account
        if account_no == '100000071001' and inserted_count <= 5:
            print(f"  {txn['Tran_Id']}: {txn['Dr_Cr_Flag']} {txn['LCY_Amt']} -> Balance: {balance_after}")
    
    cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
    conn.commit()
    
    print(f"\n{'='*50}")
    print(f"Successfully inserted {inserted_count} records!")
    print(f"{'='*50}\n")
    
    # Show results
    cursor.execute("SELECT COUNT(*) AS total FROM txn_hist_acct")
    total = cursor.fetchone()['total']
    print(f"Total records in txn_hist_acct: {total}")
    
    cursor.execute("""
        SELECT 
            TRAN_TYPE,
            COUNT(*) AS count,
            SUM(TRAN_AMT) AS total_amt
        FROM txn_hist_acct
        GROUP BY TRAN_TYPE
    """)
    print("\nBreakdown by type:")
    for row in cursor:
        print(f"  {row['TRAN_TYPE']}: {row['count']} transactions, Total: {row['total_amt']}")
    
    # Show sample for test account
    cursor.execute("""
        SELECT 
            TRAN_DATE, TRAN_TYPE, TRAN_AMT, BALANCE_AFTER_TRAN
        FROM txn_hist_acct
        WHERE ACC_No = '100000071001'
        ORDER BY TRAN_DATE
        LIMIT 10
    """)
    print("\nSample for account 100000071001:")
    print(f"{'Date':<12} {'Type':<5} {'Amount':>12} {'Balance':>15}")
    print("-" * 50)
    for row in cursor:
        print(f"{str(row['TRAN_DATE']):<12} {row['TRAN_TYPE']:<5} {row['TRAN_AMT']:>12.2f} {row['BALANCE_AFTER_TRAN']:>15.2f}")
    
    cursor.close()
    conn.close()
    
    print("\n" + "=" * 50)
    print("Population completed successfully!")
    print("=" * 50)

except mysql.connector.Error as e:
    print(f"\nError: {e}")
except Exception as e:
    print(f"\nUnexpected error: {e}")

