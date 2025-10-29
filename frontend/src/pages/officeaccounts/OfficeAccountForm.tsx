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
  FormControlLabel,
  FormHelperText,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  Switch,
  TextField
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createOfficeAccount, getOfficeAccountByAccountNo, updateOfficeAccount } from '../../api/officeAccountService';
import { getAllSubProducts } from '../../api/subProductService';
import { FormSection, PageHeader } from '../../components/common';
import type { OfficeAccountRequestDTO } from '../../types';
import { SubProductStatus, AccountStatus } from '../../types';

const OfficeAccountForm = () => {
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
  } = useForm<OfficeAccountRequestDTO>({
    defaultValues: {
      subProductId: 0,
      acctName: '',
      dateOpening: new Date().toISOString().split('T')[0], // Today's date
      dateClosure: undefined,
      branchCode: '001', // Default branch code
      accountStatus: AccountStatus.ACTIVE,
      reconciliationRequired: false,
    }
  });

  // Get subproducts for dropdown
  const { data: subProductsData, isLoading: isLoadingSubProducts } = useQuery({
    queryKey: ['subproducts', { page: 0, size: 100 }],
    queryFn: () => getAllSubProducts(0, 100),
  });

  // Get account data if editing
  const { data: accountData, isLoading: isLoadingAccount } = useQuery({
    queryKey: ['officeAccount', accountNo],
    queryFn: () => getOfficeAccountByAccountNo(accountNo!),
    enabled: isEdit && Boolean(accountNo),
  });

  // Get selected values
  const selectedSubProductId = watch('subProductId');

  // Find selected subproduct
  const selectedSubProduct = subProductsData?.content.find(s => s.subProductId === selectedSubProductId);

  // Create account mutation
  const createAccountMutation = useMutation({
    mutationFn: createOfficeAccount,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['officeAccounts'] });
      
      // Show dialog with account number instead of toast
      setCreatedAccountNo(data.accountNo);
      setSuccessDialogOpen(true);
    },
    onError: (error: Error) => {
      toast.error(`Failed to create office account: ${error.message}`);
    },
  });

  // Update account mutation
  const updateAccountMutation = useMutation({
    mutationFn: ({ accountNo, data }: { accountNo: string; data: OfficeAccountRequestDTO }) =>
      updateOfficeAccount(accountNo, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['officeAccounts'] });
      queryClient.invalidateQueries({ queryKey: ['officeAccount', accountNo] });
      toast.success('Office account updated successfully');
      navigate('/office-accounts');
    },
    onError: (error: Error) => {
      toast.error(`Failed to update office account: ${error.message}`);
    },
  });

  // Populate form when account data is loaded (edit mode)
  useEffect(() => {
    if (accountData && isEdit) {
      setValue('subProductId', accountData.subProductId);
      setValue('acctName', accountData.acctName);
      setValue('dateOpening', accountData.dateOpening);
      setValue('dateClosure', accountData.dateClosure || undefined);
      setValue('branchCode', accountData.branchCode);
      setValue('accountStatus', accountData.accountStatus);
      setValue('reconciliationRequired', accountData.reconciliationRequired);
    }
  }, [accountData, isEdit, setValue]);

  // Handle form submission
  const onSubmit = (data: OfficeAccountRequestDTO) => {
    if (isEdit && accountNo) {
      updateAccountMutation.mutate({ accountNo, data });
    } else {
      createAccountMutation.mutate(data);
    }
  };

  // Handle success dialog close
  const handleSuccessDialogClose = () => {
    setSuccessDialogOpen(false);
    navigate('/office-accounts');
  };

  // Filter active subproducts - only show non-customer products (Customer_Product_Flag = 'N')
  // and exclude interest-related GLs (1301%, 1401%, 2301%, 2401%)
  const activeSubProducts = subProductsData?.content.filter(
    subProduct => subProduct.subProductStatus === SubProductStatus.ACTIVE && 
                  subProduct.customerProductFlag === false &&
                  !subProduct.cumGLNum.startsWith('1301') &&
                  !subProduct.cumGLNum.startsWith('1401') &&
                  !subProduct.cumGLNum.startsWith('2301') &&
                  !subProduct.cumGLNum.startsWith('2401')
  ) || [];

  const isLoading = isLoadingSubProducts || (isEdit && isLoadingAccount);
  const isSubmitting = createAccountMutation.isPending || updateAccountMutation.isPending;

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader 
        title={isEdit ? 'Edit Office Account' : 'New Office Account'}
        subtitle={isEdit ? `Editing office account: ${accountNo}` : 'Create a new office account (non-customer account)'}
        buttonText="Back to Office Accounts"
        buttonLink="/office-accounts"
        startIcon={<ArrowBackIcon />}
      />

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Box component="form" onSubmit={handleSubmit(onSubmit)} sx={{ mt: 3 }}>
          <Grid container spacing={3}>
            {/* Account Information Section */}
            <Grid item xs={12}>
              <FormSection title="Account Information">
                <Grid container spacing={3}>
                  {/* Account Number (Read-only in edit mode) */}
                  {isEdit && (
                    <Grid item xs={12} md={6}>
                      <TextField
                        fullWidth
                        label="Account Number"
                        value={accountData?.accountNo || ''}
                        InputProps={{
                          readOnly: true,
                        }}
                        helperText="Account number is auto-generated and cannot be changed"
                      />
                    </Grid>
                  )}

                  {/* Sub Product */}
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="subProductId"
                      control={control}
                      rules={{ 
                        required: 'Sub Product is required',
                        min: { value: 1, message: 'Please select a sub product' }
                      }}
                      render={({ field }) => (
                        <Autocomplete
                          options={activeSubProducts}
                          getOptionLabel={(option) => `${option.subProductName} (${option.subProductCode})`}
                          value={selectedSubProduct || null}
                          onChange={(_, newValue) => {
                            field.onChange(newValue?.subProductId || 0);
                          }}
                          loading={isLoadingSubProducts}
                          renderInput={(params) => (
                            <TextField
                              {...params}
                              label="Sub Product *"
                              error={!!errors.subProductId}
                              helperText={errors.subProductId?.message}
                              InputProps={{
                                ...params.InputProps,
                                endAdornment: (
                                  <>
                                    {isLoadingSubProducts ? <CircularProgress color="inherit" size={20} /> : null}
                                    {params.InputProps.endAdornment}
                                  </>
                                ),
                              }}
                            />
                          )}
                        />
                      )}
                    />
                  </Grid>

                  {/* Account Name */}
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="acctName"
                      control={control}
                      rules={{ 
                        required: 'Account Name is required',
                        maxLength: { value: 100, message: 'Account Name cannot exceed 100 characters' }
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          fullWidth
                          label="Account Name *"
                          error={!!errors.acctName}
                          helperText={errors.acctName?.message}
                        />
                      )}
                    />
                  </Grid>

                  {/* Branch Code */}
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="branchCode"
                      control={control}
                      rules={{ 
                        required: 'Branch Code is required',
                        maxLength: { value: 10, message: 'Branch Code cannot exceed 10 characters' }
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          fullWidth
                          label="Branch Code *"
                          error={!!errors.branchCode}
                          helperText={errors.branchCode?.message}
                        />
                      )}
                    />
                  </Grid>

                  {/* Date Opening */}
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="dateOpening"
                      control={control}
                      rules={{ required: 'Date of Opening is required' }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          fullWidth
                          type="date"
                          label="Date of Opening *"
                          InputLabelProps={{ shrink: true }}
                          error={!!errors.dateOpening}
                          helperText={errors.dateOpening?.message}
                        />
                      )}
                    />
                  </Grid>

                  {/* Date Closure */}
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="dateClosure"
                      control={control}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          fullWidth
                          type="date"
                          label="Date of Closure"
                          InputLabelProps={{ shrink: true }}
                          error={!!errors.dateClosure}
                          helperText={errors.dateClosure?.message}
                        />
                      )}
                    />
                  </Grid>

                  {/* Account Status */}
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="accountStatus"
                      control={control}
                      rules={{ required: 'Account Status is required' }}
                      render={({ field }) => (
                        <FormControl fullWidth error={!!errors.accountStatus}>
                          <InputLabel>Account Status *</InputLabel>
                          <Select
                            {...field}
                            label="Account Status *"
                          >
                            <MenuItem value={AccountStatus.ACTIVE}>Active</MenuItem>
                            <MenuItem value={AccountStatus.INACTIVE}>Inactive</MenuItem>
                            <MenuItem value={AccountStatus.CLOSED}>Closed</MenuItem>
                          </Select>
                          {errors.accountStatus && (
                            <FormHelperText>{errors.accountStatus.message}</FormHelperText>
                          )}
                        </FormControl>
                      )}
                    />
                  </Grid>

                  {/* Reconciliation Required */}
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="reconciliationRequired"
                      control={control}
                      render={({ field }) => (
                        <FormControlLabel
                          control={
                            <Switch
                              checked={field.value}
                              onChange={field.onChange}
                              color="primary"
                            />
                          }
                          label="Reconciliation Required"
                        />
                      )}
                    />
                  </Grid>
                </Grid>
              </FormSection>
            </Grid>

            {/* Sub Product Information (Read-only) */}
            {selectedSubProduct && (
              <Grid item xs={12}>
                <FormSection title="Sub Product Information">
                  <Grid container spacing={3}>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        label="Sub Product Code"
                        value={selectedSubProduct.subProductCode}
                        InputProps={{ readOnly: true }}
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        label="Sub Product Name"
                        value={selectedSubProduct.subProductName}
                        InputProps={{ readOnly: true }}
                      />
                    </Grid>
                    <Grid item xs={12} md={4}>
                      <TextField
                        fullWidth
                        label="GL Number"
                        value={selectedSubProduct.cumGLNum}
                        InputProps={{ readOnly: true }}
                      />
                    </Grid>
                  </Grid>
                </FormSection>
              </Grid>
            )}

            {/* Audit Information Section */}
            <Grid item xs={12}>
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
            </Grid>

            {/* Action Buttons */}
            <Grid item xs={12}>
              <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
                <Button
                  component={RouterLink}
                  to="/office-accounts"
                  variant="outlined"
                  disabled={isSubmitting}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="contained"
                  startIcon={isSubmitting ? <CircularProgress size={20} /> : <SaveIcon />}
                  disabled={isSubmitting}
                >
                  {isSubmitting 
                    ? (isEdit ? 'Updating...' : 'Creating...') 
                    : (isEdit ? 'Update Office Account' : 'Create Office Account')
                  }
                </Button>
              </Box>
            </Grid>
          </Grid>
        </Box>
      )}

      {/* Success Dialog */}
      <Dialog
        open={successDialogOpen}
        onClose={handleSuccessDialogClose}
        aria-labelledby="success-dialog-title"
        aria-describedby="success-dialog-description"
      >
        <DialogTitle id="success-dialog-title">
          Office Account Created Successfully
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="success-dialog-description">
            <strong>Account Number: {createdAccountNo}</strong>
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleSuccessDialogClose} variant="contained" autoFocus>
            OK
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default OfficeAccountForm;
