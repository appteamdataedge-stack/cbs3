/**
 * Transaction API service
 */
import type { TransactionRequestDTO, TransactionResponseDTO, Page, AccountBalanceDTO } from '../types';
import { apiRequest } from './apiClient';

const TRANSACTIONS_ENDPOINT = '/transactions';
const ACCOUNTS_ENDPOINT = '/accounts';

/**
 * Create a new transaction
 */
export const createTransaction = async (transaction: TransactionRequestDTO): Promise<TransactionResponseDTO> => {
  return apiRequest<TransactionResponseDTO>({
    method: 'POST',
    url: `${TRANSACTIONS_ENDPOINT}/entry`,
    data: transaction,
  });
};

/**
 * Get all transactions with pagination
 */
export const getAllTransactions = async (page = 0, size = 10, sort?: string): Promise<Page<TransactionResponseDTO>> => {
  let url = `${TRANSACTIONS_ENDPOINT}?page=${page}&size=${size}`;
  if (sort) {
    url += `&sort=${sort}`;
  }
  
  return apiRequest<Page<TransactionResponseDTO>>({
    method: 'GET',
    url,
  });
};

/**
 * Get transaction by ID
 */
export const getTransactionById = async (tranId: string): Promise<TransactionResponseDTO> => {
  return apiRequest<TransactionResponseDTO>({
    method: 'GET',
    url: `${TRANSACTIONS_ENDPOINT}/${tranId}`,
  });
};

/**
 * Post a transaction (move from Entry to Posted status)
 */
export const postTransaction = async (tranId: string): Promise<TransactionResponseDTO> => {
  return apiRequest<TransactionResponseDTO>({
    method: 'POST',
    url: `${TRANSACTIONS_ENDPOINT}/${tranId}/post`,
  });
};

/**
 * Get account overdraft status
 */
export const getAccountOverdraftStatus = async (accountNo: string): Promise<{
  accountNo: string;
  accountName: string;
  glNum: string;
  isOverdraftAccount: boolean;
  isCustomerAccount: boolean;
  isAssetAccount: boolean;
  isLiabilityAccount: boolean;
}> => {
  return apiRequest({
    method: 'GET',
    url: `${TRANSACTIONS_ENDPOINT}/account/${accountNo}/overdraft-status`,
  });
};

/**
 * Verify a transaction (move from Posted to Verified status)
 */
export const verifyTransaction = async (tranId: string): Promise<TransactionResponseDTO> => {
  return apiRequest<TransactionResponseDTO>({
    method: 'POST',
    url: `${TRANSACTIONS_ENDPOINT}/${tranId}/verify`,
  });
};

/**
 * Reverse a transaction
 */
export const reverseTransaction = async (tranId: string, reason?: string): Promise<TransactionResponseDTO> => {
  const url = reason 
    ? `${TRANSACTIONS_ENDPOINT}/${tranId}/reverse?reason=${encodeURIComponent(reason)}`
    : `${TRANSACTIONS_ENDPOINT}/${tranId}/reverse`;
    
  return apiRequest<TransactionResponseDTO>({
    method: 'POST',
    url,
  });
};

/**
 * Get computed balance for an account
 * Returns: Available Balance + Credits - Debits from today's transactions
 */
export const getAccountBalance = async (accountNo: string): Promise<AccountBalanceDTO> => {
  return apiRequest<AccountBalanceDTO>({
    method: 'GET',
    url: `${ACCOUNTS_ENDPOINT}/${accountNo}/balance`,
  });
};

/**
 * Get the system date used for transactions
 */
export const getTransactionSystemDate = async (): Promise<{
  systemDate: string;
}> => {
  return apiRequest({
    method: 'GET',
    url: `${TRANSACTIONS_ENDPOINT}/default-value-date`,
  });
};
