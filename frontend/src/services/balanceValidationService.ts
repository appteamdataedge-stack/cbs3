/**
 * Balance Validation Service
 * 
 * Provides helper functions for validating account balances and debit transactions
 * according to the business rules:
 * - Liability accounts cannot go negative
 * - Asset accounts cannot go negative except OD/CC accounts
 * - Available balance calculation for debit validation
 */

export interface AccountBalance {
  accountNo: string;
  currentBalance: number;
  availableBalance: number;
  accountType: 'LIABILITY' | 'ASSET';
  isOverdraftAccount?: boolean;
}

export interface DebitValidationResult {
  isValid: boolean;
  message?: string;
  availableBalance: number;
  requestedAmount: number;
}

/**
 * Determine if a GL number represents a liability account
 * Liability GL numbers start with 1
 */
export function isLiabilityGL(glNum: string): boolean {
  if (!glNum || glNum.length === 0) {
    return false;
  }
  return glNum.startsWith('1');
}

/**
 * Determine if a GL number represents an asset account
 * Asset GL numbers start with 2
 */
export function isAssetGL(glNum: string): boolean {
  if (!glNum || glNum.length === 0) {
    return false;
  }
  return glNum.startsWith('2');
}

/**
 * Determine if a GL number represents a customer account
 * Customer account GL numbers have 2nd digit = 1
 */
export function isCustomerAccountGL(glNum: string): boolean {
  if (!glNum || glNum.length < 2) {
    return false;
  }
  return glNum.charAt(1) === '1';
}

/**
 * Determine if an account is an overdraft/credit account
 * OD/CC accounts have product type code = 5 (9th digit of account number)
 */
export function isOverdraftAccount(accountNo: string): boolean {
  if (!accountNo || accountNo.length < 9) {
    return false;
  }
  return accountNo.charAt(8) === '5';
}

/**
 * Determine account type from GL number
 */
export function getAccountType(glNum: string): 'LIABILITY' | 'ASSET' {
  if (isLiabilityGL(glNum)) {
    return 'LIABILITY';
  } else if (isAssetGL(glNum)) {
    return 'ASSET';
  }
  throw new Error(`Cannot determine account type for GL: ${glNum}`);
}

/**
 * Validate if a debit transaction can be performed on an account
 * 
 * Business Rules:
 * - Liability accounts: Cannot go negative
 * - Asset accounts: Cannot go negative except OD/CC accounts
 * - Check available balance before allowing debit
 */
export function validateDebitTransaction(
  accountBalance: AccountBalance,
  debitAmount: number
): DebitValidationResult {
  const { availableBalance, accountType, isOverdraftAccount } = accountBalance;
  
  // Check if debit amount exceeds available balance
  if (debitAmount > availableBalance) {
    return {
      isValid: false,
      message: 'Insufficient balance',
      availableBalance,
      requestedAmount: debitAmount
    };
  }
  
  // Calculate balance after debit
  const balanceAfterDebit = availableBalance - debitAmount;
  
  // For liability accounts, never allow negative balance
  if (accountType === 'LIABILITY') {
    if (balanceAfterDebit < 0) {
      return {
        isValid: false,
        message: 'Cannot allow negative balance for liability account',
        availableBalance,
        requestedAmount: debitAmount
      };
    }
  }
  
  // For asset accounts, only OD/CC accounts can go negative
  if (accountType === 'ASSET') {
    if (!isOverdraftAccount && balanceAfterDebit < 0) {
      return {
        isValid: false,
        message: 'Cannot allow negative balance for non-overdraft asset account',
        availableBalance,
        requestedAmount: debitAmount
      };
    }
  }
  
  return {
    isValid: true,
    availableBalance,
    requestedAmount: debitAmount
  };
}

/**
 * Create account balance object from account data
 */
export function createAccountBalance(
  accountNo: string,
  currentBalance: number,
  availableBalance: number,
  glNum: string
): AccountBalance {
  return {
    accountNo,
    currentBalance,
    availableBalance,
    accountType: getAccountType(glNum),
    isOverdraftAccount: isOverdraftAccount(accountNo)
  };
}

/**
 * Format balance for display
 */
export function formatBalance(amount: number, currency: string = 'USD'): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 2
  }).format(amount);
}

/**
 * Format account type for display
 */
export function formatAccountType(accountType: 'LIABILITY' | 'ASSET'): string {
  return accountType === 'LIABILITY' ? 'Liability' : 'Asset';
}

/**
 * Get account type color for UI display
 */
export function getAccountTypeColor(accountType: 'LIABILITY' | 'ASSET'): string {
  return accountType === 'LIABILITY' ? '#1976d2' : '#388e3c'; // Blue for liability, Green for asset
}
