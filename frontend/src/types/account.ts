/**
 * Account related type definitions
 */

// Combined account type for transaction forms (Customer + Office)
export interface CombinedAccountDTO {
  accountNo: string;
  acctName: string;
  accountType: 'Customer' | 'Office';
  displayName: string;
  // Include all fields from both account types
  subProductId?: number;
  subProductName?: string;
  glNum?: string;
  custId?: number;
  custName?: string;
  dateOpening?: string;
  tenor?: number;
  dateMaturity?: string;
  dateClosure?: string;
  branchCode?: string;
  accountStatus?: AccountStatus;
  currentBalance?: number;
  availableBalance?: number;
  makerId?: string;
  entryDate?: string;
  entryTime?: string;
  verifierId?: string;
  verificationDate?: string;
  verificationTime?: string;
  verified?: boolean;
}

// Account status enum
export enum AccountStatus {
  ACTIVE = 'Active',
  INACTIVE = 'Inactive', 
  CLOSED = 'Closed',
  DORMANT = 'Dormant'
}

// Customer account request DTO
export interface CustomerAccountRequestDTO {
  custId: number;
  subProductId: number;
  custName?: string;
  acctName: string;
  dateOpening: string; // LocalDate as ISO string
  tenor?: number;
  dateMaturity?: string; // LocalDate as ISO string
  dateClosure?: string; // LocalDate as ISO string
  branchCode: string;
  accountStatus: AccountStatus;
  loanLimit?: number;  // Loan/Limit Amount for Asset-side accounts (GL starting with "2")
}

// Customer account response DTO
export interface CustomerAccountResponseDTO {
  accountNo: string;
  subProductId: number;
  subProductName?: string;
  glNum?: string;
  custId: number;
  custName?: string;
  acctName: string;
  dateOpening: string; // LocalDate as ISO string
  tenor?: number;
  dateMaturity?: string; // LocalDate as ISO string
  dateClosure?: string; // LocalDate as ISO string
  branchCode: string;
  accountStatus: AccountStatus;
  currentBalance?: number;          // Static balance from acct_bal table
  availableBalance?: number;        // Previous day opening balance (for Liability) or includes loan limit (for Asset)
  computedBalance?: number;         // Real-time computed balance (Prev Day + Credits - Debits)
  interestAccrued?: number;         // Latest closing balance from acct_bal_accrual
  loanLimit?: number;               // Loan/Limit Amount for Asset-side accounts (GL starting with "2")
  message?: string; // Optional message from API
}

// Office account request DTO
export interface OfficeAccountRequestDTO {
  subProductId: number;
  acctName: string;
  dateOpening: string; // LocalDate as ISO string
  dateClosure?: string; // LocalDate as ISO string
  branchCode: string;
  accountStatus: AccountStatus;
  reconciliationRequired: boolean;
}

// Office account response DTO
export interface OfficeAccountResponseDTO {
  accountNo: string;
  subProductId: number;
  subProductName?: string;
  glNum?: string;
  acctName: string;
  dateOpening: string; // LocalDate as ISO string
  dateClosure?: string; // LocalDate as ISO string
  branchCode: string;
  accountStatus: AccountStatus;
  reconciliationRequired: boolean;
}
