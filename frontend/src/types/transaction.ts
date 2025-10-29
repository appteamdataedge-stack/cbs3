/**
 * Transaction related type definitions
 */

// Debit/Credit flag enum (D = Debit, C = Credit)
export enum DrCrFlag {
  D = 'D',
  C = 'C'
}

// Transaction line DTO
export interface TransactionLineDTO {
  accountNo: string;
  drCrFlag: DrCrFlag;
  tranCcy: string;
  fcyAmt: number;
  exchangeRate: number;
  lcyAmt: number;
  udf1?: string;
}

// Transaction request DTO
export interface TransactionRequestDTO {
  valueDate: string; // ISO date string
  narration?: string;
  lines: TransactionLineDTO[];
}

// Transaction line response DTO
export interface TransactionLineResponseDTO {
  tranId: string;
  accountNo: string;
  accountName?: string;
  drCrFlag: DrCrFlag;
  tranCcy: string;
  fcyAmt: number;
  exchangeRate: number;
  lcyAmt: number;
  udf1?: string;
}

// Transaction status enum
export enum TransactionStatus {
  Entry = 'Entry',
  Posted = 'Posted',
  Verified = 'Verified'
}

// Transaction response DTO
export interface TransactionResponseDTO {
  tranId: string;
  tranDate: string; // ISO date string
  valueDate: string; // ISO date string
  narration?: string;
  lines: TransactionLineResponseDTO[];
  balanced: boolean;
  status: string;
}

// Account balance DTO
export interface AccountBalanceDTO {
  accountNo: string;
  accountName: string;
  availableBalance: number;  // Previous day opening balance
  currentBalance: number;    // Current day balance from acct_bal
  todayDebits: number;       // Current day debit transactions
  todayCredits: number;      // Current day credit transactions
  computedBalance: number;    // Previous day opening + current day credits - current day debits
}
