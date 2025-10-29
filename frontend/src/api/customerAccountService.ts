/**
 * Customer Account API service
 */
import type { CustomerAccountRequestDTO, CustomerAccountResponseDTO, Page } from '../types';
import { apiRequest } from './apiClient';

const ACCOUNTS_ENDPOINT = '/accounts/customer';

/**
 * Get all customer accounts with pagination
 */
export const getAllCustomerAccounts = async (page = 0, size = 10, sort?: string): Promise<Page<CustomerAccountResponseDTO>> => {
  let url = `${ACCOUNTS_ENDPOINT}?page=${page}&size=${size}`;
  if (sort) {
    url += `&sort=${sort}`;
  }
  
  console.log('Fetching customer accounts from URL:', url);
  try {
    const response = await apiRequest<Page<CustomerAccountResponseDTO>>({
      method: 'GET',
      url,
    });
    console.log('Customer accounts response:', response);
    return response;
  } catch (error) {
    console.error('Error fetching customer accounts:', error);
    throw error;
  }
};

/**
 * Get customer account by account number
 */
export const getCustomerAccountByAccountNo = async (accountNo: string): Promise<CustomerAccountResponseDTO> => {
  return apiRequest<CustomerAccountResponseDTO>({
    method: 'GET',
    url: `${ACCOUNTS_ENDPOINT}/${accountNo}`,
  });
};

/**
 * Create a new customer account
 */
export const createCustomerAccount = async (account: CustomerAccountRequestDTO): Promise<CustomerAccountResponseDTO> => {
  return apiRequest<CustomerAccountResponseDTO>({
    method: 'POST',
    url: ACCOUNTS_ENDPOINT,
    data: account,
  });
};

/**
 * Update an existing customer account
 */
export const updateCustomerAccount = async (accountNo: string, account: CustomerAccountRequestDTO): Promise<CustomerAccountResponseDTO> => {
  return apiRequest<CustomerAccountResponseDTO>({
    method: 'PUT',
    url: `${ACCOUNTS_ENDPOINT}/${accountNo}`,
    data: account,
  });
};

/**
 * Close a customer account
 */
export const closeCustomerAccount = async (accountNo: string): Promise<CustomerAccountResponseDTO> => {
  return apiRequest<CustomerAccountResponseDTO>({
    method: 'POST',
    url: `${ACCOUNTS_ENDPOINT}/${accountNo}/close`,
  });
};
