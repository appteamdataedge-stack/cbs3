/**
 * API service for Batch Job operations
 */
import { apiRequest } from './apiClient';

// Type definitions
export interface BatchJob7ExecuteResponse {
  success: boolean;
  message: string;
  reportDate: string;
  trialBalanceFileName: string;
  balanceSheetFileName: string;
}

export interface ErrorResponse {
  success: false;
  message: string;
  timestamp?: string;
}

/**
 * Execute Batch Job 7 (Financial Reports Generation)
 */
export const executeBatchJob7 = async (date?: string): Promise<BatchJob7ExecuteResponse> => {
  try {
    const response = await apiRequest<BatchJob7ExecuteResponse>({
      method: 'POST',
      url: '/admin/eod/batch-job-7/execute',
      params: date ? { date } : undefined
    });
    return response;
  } catch (error) {
    console.error('Failed to execute Batch Job 7:', error);
    throw error;
  }
};

/**
 * Download Trial Balance CSV file
 */
export const downloadTrialBalance = async (reportDate: string): Promise<void> => {
  try {
    const response = await apiRequest<Blob>({
      method: 'GET',
      url: `/admin/eod/batch-job-7/download/trial-balance/${reportDate}`,
      responseType: 'blob'
    });
    
    // Create blob from response
    const blob = new Blob([response], { type: 'text/csv' });
    
    // Create download link
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `TrialBalance_${reportDate}.csv`;
    
    // Trigger download
    document.body.appendChild(link);
    link.click();
    
    // Cleanup
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error('Failed to download Trial Balance:', error);
    throw error;
  }
};

/**
 * Download Balance Sheet CSV file
 */
export const downloadBalanceSheet = async (reportDate: string): Promise<void> => {
  try {
    const response = await apiRequest<Blob>({
      method: 'GET',
      url: `/admin/eod/batch-job-7/download/balance-sheet/${reportDate}`,
      responseType: 'blob'
    });
    
    // Create blob from response
    const blob = new Blob([response], { type: 'text/csv' });
    
    // Create download link
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `BalanceSheet_${reportDate}.csv`;
    
    // Trigger download
    document.body.appendChild(link);
    link.click();
    
    // Cleanup
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error('Failed to download Balance Sheet:', error);
    throw error;
  }
};

/**
 * Error handler for batch job operations
 */
export const handleBatchJobError = (error: any): string => {
  // Check if axios error
  if (error.response) {
    const status = error.response.status;
    const data = error.response.data;
    
    switch (status) {
      case 400:
        return data.message || 'Invalid request parameters';
      case 404:
        return 'Report files not found. Please try generating again.';
      case 500:
        return data.message || 'Server error while generating reports';
      default:
        return data.message || 'An error occurred';
    }
  }
  
  // Network error
  if (error.request) {
    return 'Network error. Please check your connection.';
  }
  
  // Generic error
  return error.message || 'An unexpected error occurred';
};
