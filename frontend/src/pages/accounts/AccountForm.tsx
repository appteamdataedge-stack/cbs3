import { ArrowBack as ArrowBackIcon, Save as SaveIcon } from '@mui/icons-material';
import {
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControl,
  FormHelperText,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  TextField
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createCustomerAccount, getCustomerAccountByAccountNo, updateCustomerAccount } from '../../api/customerAccountService';
import { getAllCustomers } from '../../api/customerService';
import { getAllSubProducts } from '../../api/subProductService';
import { FormSection, PageHeader } from '../../components/common';
import type { CustomerAccountRequestDTO, CustomerResponseDTO, SubProductResponseDTO } from '../../types';
import { CustomerType, SubProductStatus, AccountStatus } from '../../types';

const AccountForm = () => {
  const { accountNo } = useParams<{ accountNo: string }>();
  const isEdit = Boolean(accountNo);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  // State for success dialog
  const [successDialogOpen, setSuccessDialogOpen] = useState(false);
  const [createdAccountNo, setCreatedAccountNo] = useState<string | null>(null);

  // Form setup with react-hook-form
  const { 
    control, 
    handleSubmit, 
    setValue,
    watch,
    formState: { errors }
  } = useForm<CustomerAccountRequestDTO>({
    defaultValues: {
      custId: 0,
      subProductId: 0,
      custName: '',
      acctName: '',
      dateOpening: new Date().toISOString().split('T')[0], // Today's date
      tenor: undefined,
      dateMaturity: undefined,
      dateClosure: undefined,
      branchCode: '001', // Default branch code
      accountStatus: AccountStatus.ACTIVE,
    }
  });

  // Get customers for dropdown
  const { data: customersData, isLoading: isLoadingCustomers } = useQuery({
    queryKey: ['customers', { page: 0, size: 100 }], // Get all customers for dropdown
    queryFn: () => getAllCustomers(0, 100),
  });

  // Get subproducts for dropdown
  const { data: subProductsData, isLoading: isLoadingSubProducts } = useQuery({
    queryKey: ['subproducts', { page: 0, size: 100 }],
    queryFn: () => getAllSubProducts(0, 100),
  });

  // Get account data if editing
  const { data: accountData, isLoading: isLoadingAccount } = useQuery({
    queryKey: ['account', accountNo],
    queryFn: () => getCustomerAccountByAccountNo(accountNo!),
    enabled: isEdit && Boolean(accountNo),
  });

  // Get selected values
  const selectedCustId = watch('custId');
  const selectedSubProductId = watch('subProductId');
  const dateOpening = watch('dateOpening');
  const tenor = watch('tenor');
  const dateMaturity = watch('dateMaturity');

  // Find selected customer and subproduct
  const selectedCustomer = customersData?.content.find(c => c.custId === selectedCustId);
  const selectedSubProduct = subProductsData?.content.find(s => s.subProductId === selectedSubProductId);

  // Identify account type based on GL_Num
  // As per BRD: Tenor and Date of Maturity fields are disabled for all running accounts (SB and CA).
  const isRunningAccount = selectedSubProduct?.cumGLNum ? (
    selectedSubProduct.cumGLNum.startsWith('110101') || // Savings Bank (SB)
    selectedSubProduct.cumGLNum.startsWith('110102')    // Current Account (CA)
  ) : false;
  
  // Identify deal-based accounts (TD, RD, OD/CC, TL)
  const isDealBasedAccount = selectedSubProduct?.cumGLNum ? (
    selectedSubProduct.cumGLNum.startsWith('110201') || // Term Deposit (TD)
    selectedSubProduct.cumGLNum.startsWith('110202') || // Recurring Deposit (RD)
    selectedSubProduct.cumGLNum.startsWith('210201') || // Overdraft/CC (OD/CC)
    selectedSubProduct.cumGLNum.startsWith('210202')    // Term Loan (TL)
  ) : false;

  // Identify Asset accounts (GL starting with "2") - require loan limit
  const isAssetAccount = selectedSubProduct?.cumGLNum ? 
    selectedSubProduct.cumGLNum.startsWith('2') : false;

  // Create account mutation
  const createAccountMutation = useMutation({
    mutationFn: createCustomerAccount,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      
      // Show dialog with account number instead of toast
      if (data.message) {
        setCreatedAccountNo(data.message);
        setSuccessDialogOpen(true);
      } else {
        toast.success('Account created successfully');
        navigate(`/accounts/${data.accountNo}`);
      }
    },
    onError: (error: Error) => {
      toast.error(`Failed to create account: ${error.message}`);
    }
  });

  // Update account mutation
  const updateAccountMutation = useMutation({
    mutationFn: (data: CustomerAccountRequestDTO) => updateCustomerAccount(accountNo!, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      queryClient.invalidateQueries({ queryKey: ['account', accountNo] });
      toast.success('Account updated successfully');
      navigate('/accounts');
    },
    onError: (error: Error) => {
      toast.error(`Failed to update account: ${error.message}`);
    }
  });

  const isLoading = createAccountMutation.isPending || updateAccountMutation.isPending || isLoadingCustomers || isLoadingSubProducts || isLoadingAccount;

  // Submit handler
  const onSubmit = (data: CustomerAccountRequestDTO) => {
    // Validate Tenor and Date of Maturity match for deal-based accounts
    if (isDealBasedAccount && data.tenor && data.dateMaturity && data.dateOpening) {
      const openingDate = new Date(data.dateOpening);
      const maturityDate = new Date(data.dateMaturity);
      const calculatedMaturity = new Date(openingDate);
      calculatedMaturity.setDate(calculatedMaturity.getDate() + Number(data.tenor));
      
      // Check if dates match (allowing 1 day tolerance for date calculation differences)
      const timeDiff = Math.abs(maturityDate.getTime() - calculatedMaturity.getTime());
      const daysDiff = Math.ceil(timeDiff / (1000 * 60 * 60 * 24));
      
      if (daysDiff > 1) {
        toast.warning("Tenor and Date of Maturity do not match. Please correct the input.");
        return;
      }
    }
    
    // Nullify tenor and dateMaturity for running accounts
    // As per BRD: Tenor and Date of Maturity fields are disabled for all running accounts (SB and CA).
    const sanitizedData = {
      ...data,
      tenor: isRunningAccount ? undefined : data.tenor,
      dateMaturity: isRunningAccount ? undefined : data.dateMaturity,
    };
    
    if (isEdit) {
      updateAccountMutation.mutate(sanitizedData);
    } else {
      createAccountMutation.mutate(sanitizedData);
    }
  };

  // Derive display name from selected customer
  const getSelectedCustomerName = (): string => {
    if (!selectedCustomer) return '';
    if (selectedCustomer.custType === CustomerType.INDIVIDUAL) {
      return `${selectedCustomer.firstName || ''} ${selectedCustomer.lastName || ''}`.trim();
    }
    return selectedCustomer.tradeName || '';
  };

  // Generate account name from current selections
  const generateAccountName = () => {
    const customerName = getSelectedCustomerName();
    if (customerName) {
      setValue('custName', customerName);
    }
    if (customerName && selectedSubProduct) {
      const accountName = `${customerName} - ${selectedSubProduct.subProductName}`;
      setValue('acctName', accountName);
    }
  };

  // Use generateAccountName when needed
  useEffect(() => {
    if (selectedCustomer && selectedSubProduct) {
      generateAccountName();
    }
  }, [selectedCustomer, selectedSubProduct]);

  // Populate form when editing
  useEffect(() => {
    if (accountData && isEdit) {
      setValue('custId', accountData.custId);
      setValue('subProductId', accountData.subProductId);
      setValue('acctName', accountData.acctName);
      setValue('dateOpening', accountData.dateOpening);
      setValue('tenor', accountData.tenor);
      setValue('dateMaturity', accountData.dateMaturity);
      setValue('dateClosure', accountData.dateClosure);
      setValue('branchCode', accountData.branchCode);
      setValue('accountStatus', accountData.accountStatus);
      setValue('loanLimit', accountData.loanLimit);
    }
  }, [accountData, isEdit, setValue]);

  // Reset tenor and dateMaturity for running accounts
  // As per BRD: Tenor and Date of Maturity fields are disabled for all running accounts (SB and CA).
  useEffect(() => {
    if (isRunningAccount) {
      setValue('tenor', undefined);
      setValue('dateMaturity', undefined);
    }
  }, [isRunningAccount, setValue]);

  // Auto-calculate Date of Maturity when Tenor changes
  // As per BRD: Date of Maturity = Date of Opening + Tenor.
  useEffect(() => {
    if (isDealBasedAccount && tenor && dateOpening && !isEdit) {
      const openingDate = new Date(dateOpening);
      const maturityDate = new Date(openingDate);
      maturityDate.setDate(maturityDate.getDate() + Number(tenor));
      setValue('dateMaturity', maturityDate.toISOString().split('T')[0]);
    }
  }, [tenor, dateOpening, isDealBasedAccount, isEdit, setValue]);

  // Auto-calculate Tenor when Date of Maturity changes
  // Alternatively, Tenor = Date of Maturity - Date of Opening.
  useEffect(() => {
    if (isDealBasedAccount && dateMaturity && dateOpening && !isEdit) {
      const openingDate = new Date(dateOpening);
      const maturityDate = new Date(dateMaturity);
      const diffTime = maturityDate.getTime() - openingDate.getTime();
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      
      if (diffDays >= 0) {
        setValue('tenor', diffDays);
      }
    }
  }, [dateMaturity, dateOpening, isDealBasedAccount, isEdit, setValue]);

  // Handle dialog close
  const handleCloseSuccessDialog = () => {
    setSuccessDialogOpen(false);
    navigate(`/accounts`);
  };

  return (
    <Box>
        <PageHeader
          title={isEdit ? "Edit Account" : "Create New Customer Account"}
          buttonText="Back to Accounts"
          buttonLink="/accounts"
          startIcon={<ArrowBackIcon />}
        />
        
        {/* Success Dialog */}
        <Dialog
          open={successDialogOpen}
          onClose={handleCloseSuccessDialog}
          aria-labelledby="alert-dialog-title"
          aria-describedby="alert-dialog-description"
        >
          <DialogTitle id="alert-dialog-title">Account Created</DialogTitle>
          <DialogContent>
            <DialogContentText id="alert-dialog-description">
              {createdAccountNo}
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={handleCloseSuccessDialog} color="primary" autoFocus>
              OK
            </Button>
          </DialogActions>
        </Dialog>

        <form onSubmit={handleSubmit(onSubmit)}>
        <FormSection title="Account Information">
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              {/* Account Number field - disabled in create mode */}
              <TextField
                label="Account Number"
                value="Will be generated"
                fullWidth
                disabled={true}
                InputProps={{
                  readOnly: true,
                }}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Controller
                name="custId"
                control={control}
                rules={{ 
                  required: 'Customer is mandatory',
                  validate: value => value > 0 || 'Please select a customer'
                }}
                render={({ field }) => (
                  <Autocomplete
                    options={customersData?.content || []}
                    getOptionLabel={(option: CustomerResponseDTO) => {
                      let displayName = '';
                      if (option.custType === CustomerType.INDIVIDUAL) {
                        displayName = `${option.firstName || ''} ${option.lastName || ''}`.trim();
                      } else {
                        displayName = option.tradeName || '';
                      }
                      return `${displayName} (ID: ${option.custId})`;
                    }}
                    value={customersData?.content.find((customer: CustomerResponseDTO) => customer.custId === field.value) || null}
                    onChange={(_, newValue: CustomerResponseDTO | null) => {
                      const customerId = newValue?.custId || 0;
                      field.onChange(customerId);
                      // Immediately set customer name from selection
                      const name = newValue
                        ? (newValue.custType === CustomerType.INDIVIDUAL
                          ? `${newValue.firstName || ''} ${newValue.lastName || ''}`.trim()
                          : newValue.tradeName || '')
                        : '';
                      if (name) {
                        setValue('custName', name);
                      }
                      // Update account name if subproduct already chosen
                      if (name && selectedSubProduct) {
                        setValue('acctName', `${name} - ${selectedSubProduct.subProductName}`);
                      }
                    }}
                    disabled={isLoading}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Customer *"
                        error={!!errors.custId}
                        helperText={errors.custId?.message}
                        placeholder="Search and select customer..."
                      />
                    )}
                    renderOption={(props, option: CustomerResponseDTO) => {
                      let displayName = '';
                      if (option.custType === CustomerType.INDIVIDUAL) {
                        displayName = `${option.firstName || ''} ${option.lastName || ''}`.trim();
                      } else {
                        displayName = option.tradeName || '';
                      }
                      return (
                        <Box component="li" {...props} key={option.custId}>
                          <Box>
                            <Box sx={{ fontWeight: 'medium' }}>
                              {displayName}
                            </Box>
                            <Box sx={{ fontSize: '0.875rem', color: 'text.secondary' }}>
                              ID: {option.custId} • {option.custType}
                            </Box>
                          </Box>
                        </Box>
                      );
                    }}
                    isOptionEqualToValue={(option: CustomerResponseDTO, value: CustomerResponseDTO) => 
                      option.custId === value.custId
                    }
                    noOptionsText="No customers found"
                    loading={isLoadingCustomers}
                  />
                )}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Controller
                name="subProductId"
                control={control}
                rules={{ 
                  required: 'SubProduct is mandatory',
                  validate: value => value > 0 || 'Please select a subproduct'
                }}
                render={({ field }) => {
                  // Filter sub-products to show ACTIVE and VERIFIED customer products
                  // If customerProductFlag is not set (undefined), we include it to avoid filtering out valid products
                  const customerSubProducts = subProductsData?.content.filter((sp: SubProductResponseDTO) => 
                    sp.subProductStatus === SubProductStatus.ACTIVE && 
                    sp.verified &&
                    (sp.customerProductFlag === true || sp.customerProductFlag === undefined)
                  ) || [];
                  
                  return (
                    <Autocomplete
                      options={customerSubProducts}
                      getOptionLabel={(option: SubProductResponseDTO) => 
                        `${option.subProductName} (${option.subProductCode})`
                      }
                      value={customerSubProducts.find((subproduct: SubProductResponseDTO) => subproduct.subProductId === field.value) || null}
                      onChange={(_, newValue: SubProductResponseDTO | null) => {
                        const subProductId = newValue?.subProductId || 0;
                        field.onChange(subProductId);
                        const name = getSelectedCustomerName();
                        if (name && newValue) {
                          setValue('acctName', `${name} - ${newValue.subProductName}`);
                        }
                      }}
                      disabled={isLoading}
                      renderInput={(params) => (
                        <TextField
                          {...params}
                          label="SubProduct *"
                          error={!!errors.subProductId}
                          helperText={errors.subProductId?.message}
                          placeholder="Search and select subproduct..."
                        />
                      )}
                      renderOption={(props, option: SubProductResponseDTO) => (
                        <Box component="li" {...props} key={option.subProductId}>
                          <Box>
                            <Box sx={{ fontWeight: 'medium' }}>
                              {option.subProductName}
                            </Box>
                            <Box sx={{ fontSize: '0.875rem', color: 'text.secondary' }}>
                              {option.subProductCode} • {option.productName}
                            </Box>
                          </Box>
                        </Box>
                      )}
                      isOptionEqualToValue={(option: SubProductResponseDTO, value: SubProductResponseDTO) => 
                        option.subProductId === value.subProductId
                      }
                      noOptionsText="No subproducts found"
                      loading={isLoadingSubProducts}
                    />
                  );
                }}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="acctName"
                control={control}
                rules={{ required: 'Account Name is mandatory' }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Account Name"
                    fullWidth
                    required
                    error={!!errors.acctName}
                    helperText={errors.acctName?.message}
                    disabled={isLoading}
                  />
                )}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="dateOpening"
                control={control}
                rules={{ required: 'Date of Opening is mandatory' }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Date of Opening"
                    type="date"
                    fullWidth
                    required
                    error={!!errors.dateOpening}
                    helperText={errors.dateOpening?.message}
                    disabled={isLoading}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />
                )}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="tenor"
                control={control}
                rules={{ 
                  min: { value: 1, message: 'Tenor must be at least 1 day' },
                  max: { value: 999, message: 'Tenor cannot exceed 999 days' }
                }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Tenor (Days)"
                    type="number"
                    fullWidth
                    error={!!errors.tenor}
                    helperText={
                      isRunningAccount 
                        ? 'Not applicable for running accounts (SB/CA)' 
                        : (errors.tenor?.message || (isDealBasedAccount ? 'Auto-calculated based on Date of Maturity' : 'Optional: Number of days'))
                    }
                    disabled={isLoading || isRunningAccount}
                    InputProps={{
                      readOnly: isRunningAccount
                    }}
                  />
                )}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="dateMaturity"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Date of Maturity"
                    type="date"
                    fullWidth
                    error={!!errors.dateMaturity}
                    helperText={
                      isRunningAccount 
                        ? 'Not applicable for running accounts (SB/CA)' 
                        : (errors.dateMaturity?.message || (isDealBasedAccount ? 'Auto-calculated based on Tenor' : 'Optional: For term deposits'))
                    }
                    disabled={isLoading || isRunningAccount}
                    InputLabelProps={{
                      shrink: true,
                    }}
                    InputProps={{
                      readOnly: isRunningAccount
                    }}
                  />
                )}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="dateClosure"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Date of Closure"
                    type="date"
                    fullWidth
                    error={!!errors.dateClosure}
                    helperText={errors.dateClosure?.message || 'Optional: When account was closed'}
                    disabled={isLoading}
                    InputLabelProps={{
                      shrink: true,
                    }}
                  />
                )}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="branchCode"
                control={control}
                rules={{ required: 'Branch Code is mandatory' }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Branch Code"
                    fullWidth
                    required
                    error={!!errors.branchCode}
                    helperText={errors.branchCode?.message}
                    disabled={isLoading}
                  />
                )}
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Controller
                name="accountStatus"
                control={control}
                rules={{ required: 'Account Status is mandatory' }}
                render={({ field }) => (
                  <FormControl fullWidth error={!!errors.accountStatus} disabled={isLoading}>
                    <InputLabel id="status-label">Account Status</InputLabel>
                    <Select
                      {...field}
                      labelId="status-label"
                      label="Account Status"
                    >
                      <MenuItem value={AccountStatus.ACTIVE}>Active</MenuItem>
                      <MenuItem value={AccountStatus.INACTIVE}>Inactive</MenuItem>
                      <MenuItem value={AccountStatus.CLOSED}>Closed</MenuItem>
                      <MenuItem value={AccountStatus.DORMANT}>Dormant</MenuItem>
                    </Select>
                    <FormHelperText>{errors.accountStatus?.message}</FormHelperText>
                  </FormControl>
                )}
              />
            </Grid>

            {/* Loan/Limit Amount - Only for Asset accounts (GL starting with "2") */}
            {isAssetAccount && (
              <Grid item xs={12} md={6}>
                <Controller
                  name="loanLimit"
                  control={control}
                  rules={{ 
                    min: { value: 0, message: 'Loan limit cannot be negative' }
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Loan/Limit Amount"
                      type="number"
                      fullWidth
                      error={!!errors.loanLimit}
                      helperText={
                        errors.loanLimit?.message || 
                        'Loan limit for Asset-side accounts (used in available balance calculation)'
                      }
                      disabled={isLoading}
                      InputProps={{
                        inputProps: { 
                          min: 0,
                          step: 0.01
                        }
                      }}
                    />
                  )}
                />
              </Grid>
            )}
          </Grid>
        </FormSection>

        {selectedSubProduct && (
          <FormSection title="SubProduct Details">
            <Grid container spacing={3}>
              <Grid item xs={12} md={4}>
                <TextField
                  label="Interest Rate"
                  value="N/A"
                  fullWidth
                  InputProps={{ readOnly: true }}
                />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField
                  label="Term"
                  value="N/A"
                  fullWidth
                  InputProps={{ readOnly: true }}
                />
              </Grid>
              <Grid item xs={12} md={4}>
                <TextField
                  label="Product Type"
                  value={selectedSubProduct.productName || ''}
                  fullWidth
                  InputProps={{ readOnly: true }}
                />
              </Grid>
            </Grid>
          </FormSection>
        )}

        {/* Audit Information Section */}
        <FormSection title="Audit Information">
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <TextField
                label="Maker ID"
                value="FRONTEND_USER"
                fullWidth
                disabled
                InputProps={{ readOnly: true }}
                helperText="User who created this record"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Entry Date"
                value={new Date().toISOString().split('T')[0]}
                fullWidth
                disabled
                InputProps={{ readOnly: true }}
                helperText="Date when record was created"
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Entry Time"
                value={new Date().toTimeString().split(' ')[0]}
                fullWidth
                disabled
                InputProps={{ readOnly: true }}
                helperText="Time when record was created"
              />
            </Grid>
          </Grid>
        </FormSection>

        <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
          <Button
            component={RouterLink}
            to="/accounts"
            variant="outlined"
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={isLoading}
            startIcon={isLoading ? <CircularProgress size={20} /> : <SaveIcon />}
          >
            Create Account
          </Button>
        </Box>
      </form>
    </Box>
  );
};

export default AccountForm;
