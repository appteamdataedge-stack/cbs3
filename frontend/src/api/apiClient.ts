/**
 * API client with Axios for backend communication
 */
import axios, { AxiosError } from 'axios';
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import type { ErrorResponse } from '../types';
import { getAuthToken, clearAuthToken } from '../utils/authUtils';

// Get API base URL from environment variable
// Ensure we're using the correct backend port
const API_BASE_URL = `${import.meta.env.VITE_API_URL || 'http://localhost:8082'}/api`;
console.log('API Base URL:', API_BASE_URL);

// Security configuration
const CSRF_HEADER = 'X-CSRF-TOKEN';
const CSRF_COOKIE = 'XSRF-TOKEN';

/**
 * Create an Axios instance with default configuration
 */
const apiClient: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'X-Requested-With': 'XMLHttpRequest',
  },
  withCredentials: true, // Include cookies in requests
});

/**
 * Get CSRF token from cookies
 */
const getCsrfToken = (): string | null => {
  const cookies = document.cookie.split(';');
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=');
    if (name === CSRF_COOKIE) {
      return value;
    }
  }
  return null;
};

/**
 * Request interceptor
 */
apiClient.interceptors.request.use(
  (config) => {
    // Add authorization header if token exists
    const token = getAuthToken();
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    
    // Add CSRF token for non-GET requests
    if (config.method !== 'get') {
      const csrfToken = getCsrfToken();
      if (csrfToken) {
        config.headers[CSRF_HEADER] = csrfToken;
      }
    }
    
    // Add timestamp to prevent caching
    const timestamp = new Date().getTime();
    if (config.params) {
      config.params._t = timestamp;
    } else {
      config.params = { _t: timestamp };
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

/**
 * Response interceptor
 */
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ErrorResponse>) => {
    // Format and throw meaningful errors
    if (error.response) {
      const { status, data } = error.response;
      
      // Handle authentication errors
      if (status === 401) {
        clearAuthToken();
        // Redirect to login page
        window.location.href = '/login';
        return Promise.reject(new Error('Your session has expired. Please log in again.'));
      }
      
      // Handle forbidden errors
      if (status === 403) {
        return Promise.reject(new Error('You do not have permission to perform this action.'));
      }
      
      // Business errors are returned with HTTP 400 status
      if (status === 400 && data) {
        return Promise.reject(new Error(data.message || 'Business validation error'));
      }
      
      // Not found errors
      if (status === 404) {
        return Promise.reject(new Error('Resource not found'));
      }
      
      // Server errors
      if (status >= 500) {
        return Promise.reject(new Error('Server error occurred. Please try again later.'));
      }
    }
    
    // Network errors or other issues
    console.error('API Request failed:', error);
    return Promise.reject(new Error(error.message || 'Cannot connect to server. Please check if the backend is running.'));
  }
);

/**
 * Generic API request function with typed responses and retry mechanism
 */
export const apiRequest = async <T>(
  config: AxiosRequestConfig, 
  retries = 2, 
  retryDelay = 1000
): Promise<T> => {
  try {
    const response: AxiosResponse<T> = await apiClient(config);
    return response.data;
  } catch (error: unknown) {
    // Only retry on network errors or 5xx server errors
    const axiosError = error as AxiosError;
    const isNetworkError = !axiosError.response;
    const isServerError = axiosError.response && axiosError.response.status >= 500;
    
    if ((isNetworkError || isServerError) && retries > 0) {
      console.log(`Retrying API request (${retries} retries left)...`);
      
      // Wait before retrying
      await new Promise(resolve => setTimeout(resolve, retryDelay));
      
      // Retry with one less retry attempt and increased delay
      return apiRequest<T>(config, retries - 1, retryDelay * 1.5);
    }
    
    throw error;
  }
};

export default apiClient;
