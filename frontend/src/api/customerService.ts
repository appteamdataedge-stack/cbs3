/**
 * Customer API service
 */
import type { CustomerRequestDTO, CustomerResponseDTO, CustomerVerificationDTO, Page } from '../types';
import { apiRequest } from './apiClient';

const CUSTOMERS_ENDPOINT = '/customers';

/**
 * Get all customers with pagination
 */
export const getAllCustomers = async (page = 0, size = 10, sort?: string, search?: string): Promise<Page<CustomerResponseDTO>> => {
  let url = `${CUSTOMERS_ENDPOINT}?page=${page}&size=${size}`;
  if (sort) {
    url += `&sort=${sort}`;
  }
  if (search) {
    url += `&search=${encodeURIComponent(search)}`;
  }
  
  return apiRequest<Page<CustomerResponseDTO>>({
    method: 'GET',
    url,
  });
};

/**
 * Get customer by ID
 */
export const getCustomerById = async (id: number): Promise<CustomerResponseDTO> => {
  return apiRequest<CustomerResponseDTO>({
    method: 'GET',
    url: `${CUSTOMERS_ENDPOINT}/${id}`,
  });
};

/**
 * Create a new customer
 */
export const createCustomer = async (customer: CustomerRequestDTO): Promise<CustomerResponseDTO> => {
  return apiRequest<CustomerResponseDTO>({
    method: 'POST',
    url: CUSTOMERS_ENDPOINT,
    data: customer,
  });
};

/**
 * Update an existing customer
 */
export const updateCustomer = async (id: number, customer: CustomerRequestDTO): Promise<CustomerResponseDTO> => {
  return apiRequest<CustomerResponseDTO>({
    method: 'PUT',
    url: `${CUSTOMERS_ENDPOINT}/${id}`,
    data: customer,
  });
};

/**
 * Verify a customer (maker-checker process)
 */
export const verifyCustomer = async (id: number, verification: CustomerVerificationDTO): Promise<CustomerResponseDTO> => {
  return apiRequest<CustomerResponseDTO>({
    method: 'POST',
    url: `${CUSTOMERS_ENDPOINT}/${id}/verify`,
    data: verification,
  });
};
