/**
 * Admin API service
 */
import type { EODResponse } from '../types';
import { apiRequest } from './apiClient';

// EOD Job Management Types
export interface EODJobStatus {
  jobNumber: number;
  jobName: string;
  status: 'pending' | 'running' | 'completed' | 'failed';
  executionTime?: string;
  recordsProcessed: number;
  errorMessage?: string;
  canExecute: boolean;
}

export interface EODJobResult {
  jobNumber: number;
  jobName: string;
  success: boolean;
  message: string;
  recordsProcessed: number;
  executionTime?: string;
}


/**
 * Run End of Day (EOD) process
 */
export const runEOD = async (date?: string): Promise<EODResponse> => {
  let url = '/api/eod/process';
  if (date) {
    url += `?date=${date}`;
  }
  
  return apiRequest<EODResponse>({
    method: 'POST',
    url,
  });
};

/**
 * Get current system date and EOD status
 */
export const getEODStatus = async (): Promise<{
  systemDate: string;
  currentDate: string;
  timestamp: string;
}> => {
  return apiRequest<{
    systemDate: string;
    currentDate: string;
    timestamp: string;
  }>({
    method: 'GET',
    url: '/admin/eod/status',
  });
};

/**
 * Execute Account Balance Update batch job (Batch Job 1)
 */
export const executeAccountBalanceUpdate = async (systemDate: string): Promise<{
  accountsProcessed: number;
  isValid: boolean;
  message: string;
}> => {
  return apiRequest({
    method: 'POST',
    url: '/admin/eod/account-balance-update',
    data: {
      systemDate: systemDate
    }
  });
};

/**
 * Execute Interest Accrual Transaction Update (Batch Job 2)
 */
export const executeInterestAccrual = async (): Promise<{
  success: boolean;
  jobName: string;
  recordsProcessed: number;
  message: string;
  systemDate: string;
}> => {
  return apiRequest({
    method: 'POST',
    url: '/admin/eod/batch/interest-accrual',
  });
};

/**
 * Execute Interest Accrual GL Movement Update (Batch Job 3)
 */
export const executeInterestAccrualGL = async (): Promise<{
  success: boolean;
  jobName: string;
  recordsProcessed: number;
  message: string;
  systemDate: string;
}> => {
  return apiRequest({
    method: 'POST',
    url: '/admin/eod/batch/interest-accrual-gl',
  });
};

/**
 * Execute GL Movement Update (Batch Job 4)
 */
export const executeGLMovement = async (): Promise<{
  success: boolean;
  jobName: string;
  recordsProcessed: number;
  message: string;
  systemDate: string;
}> => {
  return apiRequest({
    method: 'POST',
    url: '/admin/eod/batch/gl-movement',
  });
};

/**
 * Execute GL Balance Update (Batch Job 5)
 */
export const executeGLBalance = async (): Promise<{
  success: boolean;
  jobName: string;
  recordsProcessed: number;
  message: string;
  systemDate: string;
}> => {
  return apiRequest({
    method: 'POST',
    url: '/admin/eod/batch/gl-balance',
  });
};

/**
 * Execute Interest Accrual Account Balance Update (Batch Job 6)
 */
export const executeInterestAccrualBalance = async (): Promise<{
  success: boolean;
  jobName: string;
  recordsProcessed: number;
  message: string;
  systemDate: string;
}> => {
  return apiRequest({
    method: 'POST',
    url: '/admin/eod/batch/interest-accrual-balance',
  });
};

/**
 * Execute Financial Reports Generation (Batch Job 7)
 */
export const executeFinancialReports = async (): Promise<{
  success: boolean;
  jobName: string;
  reportsGenerated: number;
  reportPaths: Record<string, string>;
  message: string;
  systemDate: string;
}> => {
  return apiRequest({
    method: 'POST',
    url: '/admin/eod/batch/financial-reports',
  });
};

/**
 * Run complete EOD process (all batch jobs)
 */
export const runCompleteEOD = async (userId: string = 'ADMIN'): Promise<{
  success: boolean;
  message: string;
  accountsProcessed: number;
  interestEntriesProcessed: number;
  glMovementsProcessed: number;
  glMovementsUpdated: number;
  glBalancesUpdated: number;
  accrualBalancesUpdated: number;
  timestamp: string;
}> => {
  return apiRequest({
    method: 'POST',
    url: `/admin/run-eod?userId=${userId}`,
  });
};

// ===== NEW EOD JOB MANAGEMENT API FUNCTIONS =====

/**
 * Get status of all EOD jobs for today
 */
export const getEODJobStatuses = async (): Promise<EODJobStatus[]> => {
  return apiRequest<EODJobStatus[]>({
    method: 'GET',
    url: '/admin/eod/jobs/status',
  });
};

/**
 * Execute a specific EOD job
 */
export const executeEODJob = async (jobNumber: number, userId: string = 'SYSTEM'): Promise<EODJobResult> => {
  return apiRequest<EODJobResult>({
    method: 'POST',
    url: `/admin/eod/jobs/execute/${jobNumber}`,
    params: { userId },
  });
};

/**
 * Check if a specific job can be executed
 */
export const canExecuteEODJob = async (jobNumber: number): Promise<boolean> => {
  return apiRequest<boolean>({
    method: 'GET',
    url: `/admin/eod/jobs/can-execute/${jobNumber}`,
  });
};

/**
 * Get detailed status of a specific job
 */
export const getEODJobStatus = async (jobNumber: number): Promise<EODJobStatus> => {
  return apiRequest<EODJobStatus>({
    method: 'GET',
    url: `/admin/eod/jobs/${jobNumber}`,
  });
};

// ============================================
// BOD (Beginning of Day) API Functions
// ============================================

/**
 * BOD Processing Result
 */
export interface BODResult {
  systemDate: string;
  pendingCountBefore: number;
  processedCount: number;
  pendingCountAfter: number;
  status: string;
  message: string;
}

/**
 * BOD Status Information
 */
export interface BODStatus {
  systemDate: string;
  pendingFutureDatedCount: number;
  pendingTransactions: any[]; // Transaction details
}

/**
 * Run BOD (Beginning of Day) processing manually
 * Processes all future-dated transactions whose value date has arrived
 */
export const runBOD = async (): Promise<BODResult> => {
  return apiRequest<BODResult>({
    method: 'POST',
    url: '/bod/run',
  });
};

/**
 * Get BOD status information
 * Returns pending future-dated transactions count and details
 */
export const getBODStatus = async (): Promise<BODStatus> => {
  return apiRequest<BODStatus>({
    method: 'GET',
    url: '/bod/status',
  });
};
