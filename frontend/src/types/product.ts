/**
 * Product related type definitions
 */

// GL Setup response DTO
export interface GLSetupResponseDTO {
  glName: string;
  layerId: number;
  layerGLNum: string;
  parentGLNum: string;
  glNum: string;
}

// Product request DTO
export interface ProductRequestDTO {
  productCode: string;
  productName: string;
  cumGLNum: string; // GL Number field
  customerProductFlag?: boolean;
  interestBearingFlag?: boolean;
  dealOrRunning?: string;
  currency?: string;
  makerId: string;
}

// Product response DTO
export interface ProductResponseDTO {
  productId: number;
  productCode: string;
  productName: string;
  cumGLNum: string; // GL Number field
  customerProductFlag?: boolean;
  interestBearingFlag?: boolean;
  dealOrRunning?: string;
  currency?: string;
  makerId: string;
  entryDate: string; // LocalDate as ISO string
  entryTime: string; // LocalTime as ISO string
  verifierId?: string;
  verificationDate?: string; // LocalDate as ISO string
  verificationTime?: string; // LocalTime as ISO string
  verified: boolean;
}
