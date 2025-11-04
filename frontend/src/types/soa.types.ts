/**
 * TypeScript types for Statement of Accounts (SOA) module
 */

/**
 * Account option for dropdown selection
 */
export interface AccountOption {
  accountNo: string;
  accountName: string;
  accountType: string; // "Customer" or "Office"
}

/**
 * SOA form data
 */
export interface SOAFormData {
  accountNo: string;
  fromDate: Date | null;
  toDate: Date | null;
  format: 'excel' | 'pdf';
}

/**
 * Date range validation response
 */
export interface DateRangeValidationResponse {
  valid: boolean;
  message: string;
}

/**
 * SOA generation request
 */
export interface SOAGenerationRequest {
  accountNo: string;
  fromDate: string; // ISO date format (YYYY-MM-DD)
  toDate: string;   // ISO date format (YYYY-MM-DD)
  format: string;
}

