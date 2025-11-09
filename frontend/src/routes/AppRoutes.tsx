import { Route, Routes } from 'react-router-dom';
import Dashboard from '../pages/Dashboard';
import CustomerList from '../pages/customers/CustomerList';
import CustomerForm from '../pages/customers/CustomerForm';
import CustomerDetails from '../pages/customers/CustomerDetails';
import ProductList from '../pages/products/ProductList';
import ProductForm from '../pages/products/ProductForm';
import ProductDetails from '../pages/products/ProductDetails';
import SubProductList from '../pages/subproducts/SubProductList';
import SubProductForm from '../pages/subproducts/SubProductForm';
import SubProductDetails from '../pages/subproducts/SubProductDetails';
import AccountList from '../pages/accounts/AccountList';
import AccountForm from '../pages/accounts/AccountForm';
import AccountDetails from '../pages/accounts/AccountDetails';
import OfficeAccountList from '../pages/officeaccounts/OfficeAccountList';
import OfficeAccountForm from '../pages/officeaccounts/OfficeAccountForm';
import OfficeAccountDetails from '../pages/officeaccounts/OfficeAccountDetails';
import TransactionList from '../pages/transactions/TransactionList';
import TransactionForm from '../pages/transactions/TransactionForm';
import StatementOfAccounts from '../pages/StatementOfAccounts';
import EOD from '../pages/admin/EOD';
import BOD from '../pages/admin/BOD';
import SystemDate from '../pages/admin/SystemDate';
import Login from '../pages/auth/Login';
import TestPage from '../pages/TestPage';
import ApiTest from '../pages/ApiTest';
import CorsTest from '../pages/CorsTest';
import ApiDebug from '../pages/ApiDebug';
import ProtectedRoute from '../components/security/ProtectedRoute';
import { Box, Typography, Button } from '@mui/material';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';

// 404 Not Found Page
const NotFound = () => (
  <Box
    sx={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '60vh',
      textAlign: 'center',
      p: 3,
    }}
  >
    <ErrorOutlineIcon sx={{ fontSize: 80, color: 'warning.main', mb: 2 }} />
    <Typography variant="h2" component="h1" gutterBottom>
      404
    </Typography>
    <Typography variant="h5" component="h2" gutterBottom>
      Page Not Found
    </Typography>
    <Typography variant="body1" color="text.secondary" paragraph>
      The page you are looking for does not exist or has been moved.
    </Typography>
    <Button 
      variant="contained" 
      color="primary" 
      onClick={() => window.location.href = '/'}
      sx={{ mt: 2 }}
    >
      Go to Dashboard
    </Button>
  </Box>
);

const AppRoutes = () => {
  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={<Login />} />
      <Route path="/test" element={<TestPage />} />
      <Route path="/api-test" element={<ApiTest />} />
      <Route path="/cors-test" element={<CorsTest />} />
      <Route path="/api-debug" element={<ApiDebug />} />
      
      {/* Protected Routes */}
      <Route element={<ProtectedRoute />}>
        {/* Dashboard */}
        <Route path="/" element={<Dashboard />} />

        {/* Customer Management */}
        <Route path="/customers" element={<CustomerList />} />
        <Route path="/customers/new" element={<CustomerForm />} />
        <Route path="/customers/edit/:id" element={<CustomerForm />} />
        <Route path="/customers/view/:id" element={<CustomerDetails />} />

        {/* Product Management */}
        <Route path="/products" element={<ProductList />} />
        <Route path="/products/new" element={<ProductForm />} />
        <Route path="/products/edit/:id" element={<ProductForm />} />
        <Route path="/products/view/:id" element={<ProductDetails />} />

        {/* SubProduct Management */}
        <Route path="/subproducts" element={<SubProductList />} />
        <Route path="/subproducts/new" element={<SubProductForm />} />
        <Route path="/subproducts/edit/:id" element={<SubProductForm />} />
        <Route path="/subproducts/view/:id" element={<SubProductDetails />} />

        {/* Account Management */}
        <Route path="/accounts" element={<AccountList />} />
        <Route path="/accounts/new" element={<AccountForm />} />
        <Route path="/accounts/edit/:accountNo" element={<AccountForm />} />
        <Route path="/accounts/:accountNo" element={<AccountDetails />} />

        {/* Office Account Management */}
        <Route path="/office-accounts" element={<OfficeAccountList />} />
        <Route path="/office-accounts/new" element={<OfficeAccountForm />} />
        <Route path="/office-accounts/edit/:accountNo" element={<OfficeAccountForm />} />
        <Route path="/office-accounts/:accountNo" element={<OfficeAccountDetails />} />

        {/* Transaction Management */}
        <Route path="/transactions" element={<TransactionList />} />
        <Route path="/transactions/new" element={<TransactionForm />} />
        <Route path="/transactions/:tranId" element={<TransactionList />} />

        {/* Statement of Accounts */}
        <Route path="/statement-of-accounts" element={<StatementOfAccounts />} />

        {/* Admin - Requires admin role */}
        <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
          <Route path="/admin/bod" element={<BOD />} />
          <Route path="/admin/eod" element={<EOD />} />
          <Route path="/admin/system-date" element={<SystemDate />} />
        </Route>
      </Route>

      {/* 404 Not Found */}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
};

export default AppRoutes;
