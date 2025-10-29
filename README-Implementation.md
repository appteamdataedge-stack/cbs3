# Money Market Module Implementation

This document outlines the implementation of the Customer and Account creation logic for the Prototype CBS – Money Market Module.

## 1. Customer ID (Cust_Id) Implementation

- **System generated (not user input)**: Implemented in `CustomerIdService.java`
- **Numeric, max length 8**: Enforced in `CustomerIdService.generateCustomerId()`
- **Prefix digit based on customer type**:
  - 1 = Individual
  - 2 = Corporate
  - 3 = Bank
- **Uniqueness**: Ensured by querying the max ID with the given prefix and incrementing
- **Label "Primary Cust Id"**: Implemented in frontend `CustomerForm.tsx`
- **Branch Code = fixed value 001**: Added to the frontend form as a read-only field

## 2. Customer Creation Rules

- **Create mode → Cust_Id field disabled**: Implemented in `CustomerForm.tsx`
- **Modify/View mode → Cust_Id visible, searchable**: Implemented in `CustomerForm.tsx`
- **On Save → show popup**: Implemented confirmation dialog in `CustomerForm.tsx`
- **Insert customer record using generated Cust_Id**: Implemented in `CustomerService.createCustomer()`

## 3. Account Number (12 digits) Implementation

- **First 8 digits = Primary Cust_Id**: Implemented in `AccountNumberService.generateCustomerAccountNumber()`
- **9th digit = Product type code**: Implemented in `AccountNumberService.determineProductTypeCode()`
  - 1 = Savings Bank (GL 110101000, sub-products 110101xxx)
  - 2 = Current Account (GL 110102000, sub-products 110102xxx)
  - 3 = Term Deposit (GL 110201000, sub-products 110201xxx)
  - 4 = Recurring Deposit
  - 5 = Overdraft / CC (GL 210201000, sub-products 210201xxx)
  - 6 = Term Loan
- **Last 3 digits = running sequence**: Implemented using `CustAcctMasterRepository.findMaxSequenceForCustomerAndProductType()`
- **Office Accounts**: Implemented in `AccountNumberService.generateOfficeAccountNumber()`

## 4. Account Creation Rules

- **Create screen → Account No. field disabled**: Implemented in `AccountForm.tsx`
- **Modify/View/Verify → Account No. input with lookup**: Not shown in this implementation (would be in edit mode)
- **Auto-insert record in Account Balance Master**: Implemented in `CustomerAccountService.createAccount()`
- **Second field = Sub Product Code**: Implemented in `AccountForm.tsx`
- **Filter options**: Implemented in `GLValidationService` for filtering

## 5. General Ledger (GL) Mapping & Validation

- **Products at Layer 3, Sub-Products at Layer 4**: Enforced in `GLValidationService`
- **Parent-child consistency**: Enforced in `GLValidationService.validateSubProductGLMapping()`
- **Correct Sub-Product GL_Num mapping**: Enforced in validation
- **Uniqueness enforcement**: Implemented in `GLValidationService`
- **Field length validation**: Implemented in `GLValidationService`
- **Interest Code and External GL Num**: Added to `SubProductService.createSubProduct()`
- **Filters implementation**: Implemented in `GLValidationService` with methods:
  - `isLiabilityGL()` - GL_Num starting with 1
  - `isAssetGL()` - GL_Num starting with 2
  - `isCustomerAccountGL()` - 2nd digit 1
  - `isOfficeAccountGL()` - 2nd digit ≠ 1

## 6. Balance & Transaction Validation

- **Liability account validation**: Implemented in `TransactionValidationService.validateDebitTransaction()`
- **Asset account validation**: Implemented in `TransactionValidationService.validateDebitTransaction()`
- **Available Balance calculation**: Implemented in `TransactionValidationService.calculateAvailableBalance()`
  - Formula: Yesterday's balance - Today's debits + Today's credits
- **Insufficient balance warning**: Implemented in `TransactionValidationService.validateDebitTransaction()`

## 7. UI / Frontend Rules

- **Cust_Id field label**: Set to "Primary Cust Id" in `CustomerForm.tsx`
- **Sub Product field placement**: Implemented in `AccountForm.tsx`
- **Branch Code = fixed 001**: Implemented in both forms
- **Dropdown filters**: Implemented in frontend
- **React + TypeScript form validation**: Implemented using react-hook-form

## 8. Implementation Details

- **Backend service logic**: Implemented in:
  - `CustomerIdService.java`
  - `AccountNumberService.java`
  - `GLValidationService.java`
  - `TransactionValidationService.java`
  - Updates to existing services

- **Frontend form logic**: Implemented in:
  - `CustomerForm.tsx`
  - `AccountForm.tsx`

- **Account Number and Cust_Id generation**: Strictly follow the specified rules

- **Example workflow**:
  1. Create → Form filled out
  2. Save → Backend generates IDs
  3. Popup confirmation → Shows generated ID
  4. Record inserted → Account Balance Master initialized with zero balance

## 9. Implementation Notes

- Used existing tables and extended functionality
- Kept APIs modular and maintained backward compatibility
- Added comments explaining changes
- Applied filters and validations consistently at both backend and frontend layers
