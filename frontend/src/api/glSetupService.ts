/**
 * GL Setup API service
 */
import type { GLSetupResponseDTO } from '../types';
import { apiRequest } from './apiClient';

const GL_SETUP_ENDPOINT = '/gl-setup';

/**
 * Get GL setups by layer ID
 */
export const getGLSetupsByLayerId = async (layerId: number): Promise<GLSetupResponseDTO[]> => {
  return apiRequest<GLSetupResponseDTO[]>({
    method: 'GET',
    url: `${GL_SETUP_ENDPOINT}/layer/${layerId}`,
  });
};

/**
 * Get Layer 4 GL numbers for interest payable/receivable accounts
 */
export const getInterestPayableReceivableLayer4GLs = async (): Promise<GLSetupResponseDTO[]> => {
  return apiRequest<GLSetupResponseDTO[]>({
    method: 'GET',
    url: `${GL_SETUP_ENDPOINT}/interest/payable-receivable/layer4`,
  });
};

/**
 * Get Layer 4 GL numbers for interest income/expenditure accounts
 */
export const getInterestIncomeExpenditureLayer4GLs = async (): Promise<GLSetupResponseDTO[]> => {
  return apiRequest<GLSetupResponseDTO[]>({
    method: 'GET',
    url: `${GL_SETUP_ENDPOINT}/interest/income-expenditure/layer4`,
  });
};
