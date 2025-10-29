import apiClient from './apiClient';

export interface InterestRateMaster {
  inttCode: string;
  inttRate: number;
  inttEffctvDate: string;
}

const INTEREST_RATE_ENDPOINT = '/interest-rates';

export const getAllInterestRates = async (): Promise<InterestRateMaster[]> => {
  const response = await apiClient.get(INTEREST_RATE_ENDPOINT);
  return response.data;
};

export const getInterestRateByCode = async (inttCode: string): Promise<InterestRateMaster> => {
  const response = await apiClient.get(`${INTEREST_RATE_ENDPOINT}/${inttCode}`);
  return response.data;
};
