/**
 * Custom hook for balance validation
 * 
 * Provides React hooks for validating account balances and debit transactions
 */

import { useState, useCallback } from 'react';
import { toast } from 'react-toastify';
import type {
  DebitValidationResult
} from '../services/balanceValidationService';
import {
  validateDebitTransaction,
  createAccountBalance,
  formatBalance,
  formatAccountType
} from '../services/balanceValidationService';

export interface UseBalanceValidationReturn {
  validateDebit: (
    accountNo: string,
    currentBalance: number,
    availableBalance: number,
    glNum: string,
    debitAmount: number
  ) => DebitValidationResult;
  showValidationError: (result: DebitValidationResult) => void;
  showValidationSuccess: (result: DebitValidationResult) => void;
  formatBalanceForDisplay: (amount: number, currency?: string) => string;
  formatAccountTypeForDisplay: (accountType: 'LIABILITY' | 'ASSET') => string;
  isLoading: boolean;
}

export function useBalanceValidation(): UseBalanceValidationReturn {
  const [isLoading, setIsLoading] = useState(false);

  const validateDebit = useCallback((
    accountNo: string,
    currentBalance: number,
    availableBalance: number,
    glNum: string,
    debitAmount: number
  ): DebitValidationResult => {
    try {
      setIsLoading(true);
      
      // Create account balance object
      const accountBalance = createAccountBalance(
        accountNo,
        currentBalance,
        availableBalance,
        glNum
      );
      
      // Validate debit transaction
      const result = validateDebitTransaction(accountBalance, debitAmount);
      
      return result;
    } catch (error) {
      console.error('Balance validation error:', error);
      return {
        isValid: false,
        message: 'Error validating transaction',
        availableBalance,
        requestedAmount: debitAmount
      };
    } finally {
      setIsLoading(false);
    }
  }, []);

  const showValidationError = useCallback((result: DebitValidationResult) => {
    if (!result.isValid && result.message) {
      toast.error(`${result.message}. Available balance: ${formatBalance(result.availableBalance)}`);
    }
  }, []);

  const showValidationSuccess = useCallback((result: DebitValidationResult) => {
    if (result.isValid) {
      toast.success(`Transaction validated. Available balance: ${formatBalance(result.availableBalance)}`);
    }
  }, []);

  const formatBalanceForDisplay = useCallback((amount: number, currency?: string) => {
    return formatBalance(amount, currency);
  }, []);

  const formatAccountTypeForDisplay = useCallback((accountType: 'LIABILITY' | 'ASSET') => {
    return formatAccountType(accountType);
  }, []);

  return {
    validateDebit,
    showValidationError,
    showValidationSuccess,
    formatBalanceForDisplay,
    formatAccountTypeForDisplay,
    isLoading
  };
}
