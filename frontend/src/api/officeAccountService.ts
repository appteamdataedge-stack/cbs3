/**
 * Office Account API service
 */
import type { OfficeAccountRequestDTO, OfficeAccountResponseDTO, Page } from '../types';
import { apiRequest } from './apiClient';

const OFFICE_ACCOUNTS_ENDPOINT = '/accounts/office';

/**
 * Get all office accounts with pagination
 */
export const getAllOfficeAccounts = async (page = 0, size = 10, sort?: string): Promise<Page<OfficeAccountResponseDTO>> => {
  let url = `${OFFICE_ACCOUNTS_ENDPOINT}?page=${page}&size=${size}`;
  if (sort) {
    url += `&sort=${sort}`;
  }
  
  console.log('Fetching office accounts from URL:', url);
  try {
    const response = await apiRequest<Page<OfficeAccountResponseDTO>>({
      method: 'GET',
      url,
    });
    console.log('Office accounts response:', response);
    return response;
  } catch (error) {
    console.error('Error fetching office accounts:', error);
    throw error;
  }
};

/**
 * Get office account by account number
 */
export const getOfficeAccountByAccountNo = async (accountNo: string): Promise<OfficeAccountResponseDTO> => {
  console.log('Fetching office account by account number:', accountNo);
  try {
    const response = await apiRequest<OfficeAccountResponseDTO>({
      method: 'GET',
      url: `${OFFICE_ACCOUNTS_ENDPOINT}/${accountNo}`,
    });
    console.log('Office account response:', response);
    return response;
  } catch (error) {
    console.error('Error fetching office account:', error);
    throw error;
  }
};

/**
 * Create a new office account
 */
export const createOfficeAccount = async (accountData: OfficeAccountRequestDTO): Promise<OfficeAccountResponseDTO> => {
  console.log('Creating office account:', accountData);
  try {
    const response = await apiRequest<OfficeAccountResponseDTO>({
      method: 'POST',
      url: OFFICE_ACCOUNTS_ENDPOINT,
      data: accountData,
    });
    console.log('Office account created:', response);
    return response;
  } catch (error) {
    console.error('Error creating office account:', error);
    throw error;
  }
};

/**
 * Update an existing office account
 */
export const updateOfficeAccount = async (accountNo: string, accountData: OfficeAccountRequestDTO): Promise<OfficeAccountResponseDTO> => {
  console.log('Updating office account:', accountNo, accountData);
  try {
    const response = await apiRequest<OfficeAccountResponseDTO>({
      method: 'PUT',
      url: `${OFFICE_ACCOUNTS_ENDPOINT}/${accountNo}`,
      data: accountData,
    });
    console.log('Office account updated:', response);
    return response;
  } catch (error) {
    console.error('Error updating office account:', error);
    throw error;
  }
};

/**
 * Close an office account
 */
export const closeOfficeAccount = async (accountNo: string): Promise<OfficeAccountResponseDTO> => {
  console.log('Closing office account:', accountNo);
  try {
    const response = await apiRequest<OfficeAccountResponseDTO>({
      method: 'POST',
      url: `${OFFICE_ACCOUNTS_ENDPOINT}/${accountNo}/close`,
    });
    console.log('Office account closed:', response);
    return response;
  } catch (error) {
    console.error('Error closing office account:', error);
    throw error;
  }
};