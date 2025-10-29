/**
 * Customer related type definitions
 */

// Enum for customer type
export enum CustomerType {
  INDIVIDUAL = 'Individual',
  CORPORATE = 'Corporate',
  BANK = 'Bank'
}

// Customer request DTO
export interface CustomerRequestDTO {
  extCustId: string;
  custType: CustomerType;
  firstName?: string;
  lastName?: string;
  tradeName?: string;
  address1?: string;
  mobile?: string;
  branchCode?: string; // Default value "001" set in the backend
  makerId: string;
}

// Customer response DTO
export interface CustomerResponseDTO {
  custId: number;
  extCustId: string;
  custType: CustomerType;
  firstName?: string;
  lastName?: string;
  tradeName?: string;
  address1?: string;
  mobile?: string;
  makerId: string;
  entryDate: string; // LocalDate as ISO string
  entryTime: string; // LocalTime as ISO string
  verifierId?: string;
  verificationDate?: string; // LocalDate as ISO string
  verificationTime?: string; // LocalTime as ISO string
  verified: boolean;
  message?: string; // Optional message from API
}

// Customer verification DTO
export interface CustomerVerificationDTO {
  verifierId: string;
}
