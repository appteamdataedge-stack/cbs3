/**
 * Authentication utilities for token management
 */

// Constants
const AUTH_TOKEN_KEY = 'auth_token';
const USER_DATA_KEY = 'user_data';
const TOKEN_EXPIRY_KEY = 'token_expiry';

// Types
export interface UserData {
  id: number | string;
  username: string;
  email?: string;
  role: string;
  permissions?: string[];
  fullName?: string;
}

/**
 * Set authentication token and related data
 */
export const setAuthData = (token: string, userData: UserData, expiresInSeconds: number = 3600): void => {
  // Store token
  localStorage.setItem(AUTH_TOKEN_KEY, token);
  
  // Store user data
  localStorage.setItem(USER_DATA_KEY, JSON.stringify(userData));
  
  // Calculate and store expiry time
  const expiryTime = new Date();
  expiryTime.setSeconds(expiryTime.getSeconds() + expiresInSeconds);
  localStorage.setItem(TOKEN_EXPIRY_KEY, expiryTime.toISOString());
};

/**
 * Get authentication token
 */
export const getAuthToken = (): string | null => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  
  // Check if token exists and is not expired
  if (token && !isTokenExpired()) {
    return token;
  }
  
  // If token is expired, clear auth data
  if (token && isTokenExpired()) {
    clearAuthToken();
  }
  
  return null;
};

/**
 * Get user data
 */
export const getUserData = (): UserData | null => {
  const userDataStr = localStorage.getItem(USER_DATA_KEY);
  
  if (userDataStr) {
    try {
      return JSON.parse(userDataStr) as UserData;
    } catch (error) {
      console.error('Error parsing user data:', error);
      return null;
    }
  }
  
  return null;
};

/**
 * Check if user is authenticated
 */
export const isAuthenticated = (): boolean => {
  return !!getAuthToken();
};

/**
 * Check if token is expired
 */
export const isTokenExpired = (): boolean => {
  const expiryTimeStr = localStorage.getItem(TOKEN_EXPIRY_KEY);
  
  if (!expiryTimeStr) {
    return true;
  }
  
  try {
    const expiryTime = new Date(expiryTimeStr);
    return expiryTime <= new Date();
  } catch (error) {
    console.error('Error parsing token expiry:', error);
    return true;
  }
};

/**
 * Clear authentication data
 */
export const clearAuthToken = (): void => {
  localStorage.removeItem(AUTH_TOKEN_KEY);
  localStorage.removeItem(USER_DATA_KEY);
  localStorage.removeItem(TOKEN_EXPIRY_KEY);
};

/**
 * Check if user has specific permission
 */
export const hasPermission = (requiredPermission: string): boolean => {
  const userData = getUserData();
  
  if (!userData || !userData.permissions) {
    return false;
  }
  
  return userData.permissions.includes(requiredPermission);
};

/**
 * Check if user has specific role
 */
export const hasRole = (requiredRole: string): boolean => {
  const userData = getUserData();
  
  if (!userData) {
    return false;
  }
  
  return userData.role === requiredRole;
};
