/**
 * Service for Statement of Accounts (SOA) API operations
 */

import apiClient from '../api/apiClient';
import type { AccountOption, DateRangeValidationResponse } from '../types/soa.types';

/**
 * Get list of all accounts for dropdown
 */
export const getAccountList = async (): Promise<AccountOption[]> => {
  try {
    const response = await apiClient.get<AccountOption[]>('/soa/accounts');
    return response.data;
  } catch (error) {
    console.error('Error fetching account list:', error);
    throw new Error('Failed to fetch account list');
  }
};

/**
 * Generate Statement of Accounts and download as Excel file
 */
export const generateSOA = async (
  accountNo: string,
  fromDate: Date,
  toDate: Date,
  format: string = 'excel'
): Promise<void> => {
  try {
    // Format dates as YYYY-MM-DD
    const fromDateStr = fromDate.toISOString().split('T')[0];
    const toDateStr = toDate.toISOString().split('T')[0];

    // Make request with responseType: 'blob' to handle binary data
    const response = await apiClient.post('/soa/generate', null, {
      params: {
        accountNo,
        fromDate: fromDateStr,
        toDate: toDateStr,
        format
      },
      responseType: 'blob'
    });

    // Get filename from Content-Disposition header or construct it
    let filename = `SOA_${accountNo}_${fromDateStr}_to_${toDateStr}.xlsx`;
    const contentDisposition = response.headers['content-disposition'];
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="?(.+)"?/);
      if (filenameMatch && filenameMatch[1]) {
        filename = filenameMatch[1];
      }
    }

    // Create Blob from response
    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });

    // Create object URL
    const url = window.URL.createObjectURL(blob);

    // Create temporary <a> element and trigger download
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();

    // Cleanup
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

  } catch (error: any) {
    console.error('Error generating SOA:', error);
    
    // Handle error response
    if (error.response && error.response.data) {
      // If error response is a Blob, convert it to text
      if (error.response.data instanceof Blob) {
        const text = await error.response.data.text();
        try {
          const errorData = JSON.parse(text);
          throw new Error(errorData.message || 'Failed to generate statement');
        } catch (parseError) {
          throw new Error('Failed to generate statement');
        }
      } else if (error.response.data.message) {
        throw new Error(error.response.data.message);
      }
    }
    
    throw new Error('Failed to generate statement. Please try again.');
  }
};

/**
 * Validate date range (6-month maximum)
 */
export const validateDateRange = async (
  fromDate: Date,
  toDate: Date
): Promise<DateRangeValidationResponse> => {
  try {
    // Format dates as YYYY-MM-DD
    const fromDateStr = fromDate.toISOString().split('T')[0];
    const toDateStr = toDate.toISOString().split('T')[0];

    const response = await apiClient.post<DateRangeValidationResponse>(
      '/soa/validate-date-range',
      null,
      {
        params: {
          fromDate: fromDateStr,
          toDate: toDateStr
        }
      }
    );

    return response.data;
  } catch (error) {
    console.error('Error validating date range:', error);
    return {
      valid: false,
      message: 'Error validating date range'
    };
  }
};

