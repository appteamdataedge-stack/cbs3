/**
 * SubProduct related type definitions
 */

// Enum for sub-product status
export enum SubProductStatus {
  ACTIVE = 'Active',
  INACTIVE = 'Inactive',
  DEACTIVE = 'Deactive'
}

// SubProduct request DTO
export interface SubProductRequestDTO {
  productId: number;
  subProductCode: string;
  subProductName: string;
  inttCode?: string;
  cumGLNum: string;
  extGLNum?: string;
  interestIncrement?: number;
  interestReceivableExpenditureGLNum?: string;  // Consolidated: expenditure for liabilities, receivable for assets (GL starting with 23, 24)
  interestIncomePayableGLNum?: string;  // Consolidated: payable for liabilities, income for assets (GL starting with 13, 14)
  subProductStatus: SubProductStatus;
  makerId: string;
}

// SubProduct response DTO
export interface SubProductResponseDTO {
  subProductId: number;
  subProductCode: string;
  subProductName: string;
  productId: number;
  productCode?: string;
  productName?: string;
  customerProductFlag?: boolean;
  inttCode?: string;
  cumGLNum: string;
  extGLNum?: string;
  interestIncrement?: number;
  interestReceivableExpenditureGLNum?: string;  // Consolidated: expenditure for liabilities, receivable for assets (GL starting with 23, 24)
  interestIncomePayableGLNum?: string;  // Consolidated: payable for liabilities, income for assets (GL starting with 13, 14)
  effectiveInterestRate?: number;
  subProductStatus: SubProductStatus;
  makerId: string;
  entryDate: string; // LocalDate as ISO string
  entryTime: string; // LocalTime as ISO string
  verifierId?: string;
  verificationDate?: string; // LocalDate as ISO string
  verificationTime?: string; // LocalTime as ISO string
  verified: boolean;
}
