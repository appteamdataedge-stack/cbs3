/**
 * Product API service
 */
import type { CustomerVerificationDTO, Page, ProductRequestDTO, ProductResponseDTO, GLSetupResponseDTO } from '../types';
import { apiRequest } from './apiClient';

const PRODUCTS_ENDPOINT = '/products';
const SUBPRODUCTS_ENDPOINT = '/subproducts';

/**
 * Get all products with pagination
 */
export const getAllProducts = async (page = 0, size = 10, sort?: string): Promise<Page<ProductResponseDTO>> => {
  let url = `${PRODUCTS_ENDPOINT}?page=${page}&size=${size}`;
  if (sort) {
    url += `&sort=${sort}`;
  }
  
  return apiRequest<Page<ProductResponseDTO>>({
    method: 'GET',
    url,
  });
};

/**
 * Get product by ID
 */
export const getProductById = async (id: number): Promise<ProductResponseDTO> => {
  return apiRequest<ProductResponseDTO>({
    method: 'GET',
    url: `${PRODUCTS_ENDPOINT}/${id}`,
  });
};

/**
 * Create a new product
 */
export const createProduct = async (product: ProductRequestDTO): Promise<ProductResponseDTO> => {
  return apiRequest<ProductResponseDTO>({
    method: 'POST',
    url: PRODUCTS_ENDPOINT,
    data: product,
  });
};

/**
 * Update an existing product
 */
export const updateProduct = async (id: number, product: ProductRequestDTO): Promise<ProductResponseDTO> => {
  return apiRequest<ProductResponseDTO>({
    method: 'PUT',
    url: `${PRODUCTS_ENDPOINT}/${id}`,
    data: product,
  });
};

/**
 * Verify a product (maker-checker process)
 */
export const verifyProduct = async (id: number, verification: CustomerVerificationDTO): Promise<ProductResponseDTO> => {
  return apiRequest<ProductResponseDTO>({
    method: 'POST',
    url: `${PRODUCTS_ENDPOINT}/${id}/verify`,
    data: verification,
  });
};

/**
 * Get Layer 3 GL options for product dropdown
 */
export const getProductGLOptions = async (): Promise<GLSetupResponseDTO[]> => {
  return apiRequest<GLSetupResponseDTO[]>({
    method: 'GET',
    url: `${PRODUCTS_ENDPOINT}/gl-options`,
  });
};

/**
 * Get all products (Layer 3 GL entries) for dropdown
 */
export const getProducts = async (): Promise<GLSetupResponseDTO[]> => {
  return apiRequest<GLSetupResponseDTO[]>({
    method: 'GET',
    url: PRODUCTS_ENDPOINT,
  });
};

/**
 * Get all subproducts (Layer 4 GL entries) for dropdown
 */
export const getSubProducts = async (): Promise<GLSetupResponseDTO[]> => {
  return apiRequest<GLSetupResponseDTO[]>({
    method: 'GET',
    url: SUBPRODUCTS_ENDPOINT,
  });
};

/**
 * Get subproducts filtered by parent GL number
 */
export const getSubProductsByParent = async (parentGlNum: string): Promise<GLSetupResponseDTO[]> => {
  return apiRequest<GLSetupResponseDTO[]>({
    method: 'GET',
    url: `${SUBPRODUCTS_ENDPOINT}/gl-options/${parentGlNum}`,
  });
};
