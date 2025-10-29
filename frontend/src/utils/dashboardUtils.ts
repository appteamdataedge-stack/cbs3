/**
 * Utility functions for the dashboard
 */
import { useQuery } from '@tanstack/react-query';
import { getAllCustomers } from '../api/customerService';
import { getAllProducts } from '../api/productService';
import { getAllSubProducts } from '../api/subProductService';
import { getAllCustomerAccounts } from '../api/customerAccountService';

/**
 * Dashboard summary hook to get counts from different entities
 */
export const useDashboardSummary = () => {
  // Customer count
  const { 
    data: customerData,
    isLoading: isCustomersLoading,
    error: customersError 
  } = useQuery({
    queryKey: ['customers', { page: 0, size: 1 }],
    queryFn: () => getAllCustomers(0, 1),
  });

  // Product count
  const { 
    data: productData,
    isLoading: isProductsLoading, 
    error: productsError 
  } = useQuery({
    queryKey: ['products', { page: 0, size: 1 }],
    queryFn: () => getAllProducts(0, 1),
  });

  // SubProduct count
  const { 
    data: subProductData,
    isLoading: isSubProductsLoading,
    error: subProductsError 
  } = useQuery({
    queryKey: ['subproducts', { page: 0, size: 1 }],
    queryFn: () => getAllSubProducts(0, 1),
  });

  // Account count
  const { 
    data: accountData,
    isLoading: isAccountsLoading,
    error: accountsError 
  } = useQuery({
    queryKey: ['accounts', { page: 0, size: 1 }],
    queryFn: () => getAllCustomerAccounts(0, 1),
  });

  const isLoading = isCustomersLoading || isProductsLoading || isSubProductsLoading || isAccountsLoading;
  const error = customersError || productsError || subProductsError || accountsError;

  return {
    customerCount: customerData?.totalElements || 0,
    productCount: productData?.totalElements || 0,
    subProductCount: subProductData?.totalElements || 0,
    accountCount: accountData?.totalElements || 0,
    isLoading,
    error,
  };
};
