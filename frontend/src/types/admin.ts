/**
 * Admin related type definitions
 */

// EOD response
export interface EODResponse {
  eodDate: string; // ISO date string
  startTime: string; // ISO datetime string
  endTime: string; // ISO datetime string
  accountsProcessed: number;
  glsProcessed: number;
  balanced: boolean;
  status: string;
  errorMessage?: string;
}
