import { ArrowBack as ArrowBackIcon, Save as SaveIcon } from '@mui/icons-material';
import {
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
  TextField,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createCustomer, getCustomerById, updateCustomer } from '../../api/customerService';
import { FormSection, PageHeader } from '../../components/common';
import type { CustomerRequestDTO, CustomerResponseDTO } from '../../types';
import { CustomerType } from '../../types';

const CustomerForm = () => {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  
  // State for success dialog
  const [successDialogOpen, setSuccessDialogOpen] = useState(false);
  const [createdCustomerId, setCreatedCustomerId] = useState<string | null>(null);

  // Form setup with react-hook-form
  const { 
    control, 
    handleSubmit, 
    setValue,
    formState: { errors },
    watch
  } = useForm<CustomerRequestDTO>({
    defaultValues: {
      extCustId: '',
      custType: CustomerType.INDIVIDUAL,
      firstName: '',
      lastName: '',
      tradeName: '',
      address1: '',
      mobile: '',
      makerId: 'FRONTEND_USER', // Default maker ID
    }
  });

  // Get customer data if editing
  const { data: customerData, isLoading: isLoadingCustomer } = useQuery({
    queryKey: ['customer', id],
    queryFn: () => getCustomerById(Number(id)),
    enabled: isEdit,
  });

  // Set form values when customer data is loaded
  useEffect(() => {
    if (customerData && isEdit) {
      // Set form values from loaded customer data
      setValue('extCustId', customerData.extCustId);
      setValue('custType', customerData.custType);
      setValue('firstName', customerData.firstName || '');
      setValue('lastName', customerData.lastName || '');
      setValue('tradeName', customerData.tradeName || '');
      setValue('address1', customerData.address1 || '');
      setValue('mobile', customerData.mobile || '');
      setValue('makerId', 'FRONTEND_USER'); // Use default for edits too
    }
  }, [customerData, isEdit, setValue]);

  // Watch custType to conditionally render fields
  const custType = watch('custType');

  // Mutations for create and update
  const createMutation = useMutation({
    mutationFn: createCustomer,
    onSuccess: (data: CustomerResponseDTO) => {
      // Invalidate customer queries to ensure all customer lists refresh
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      // Show dialog with customer ID instead of toast
      if (data.message) {
        setCreatedCustomerId(data.message);
        setSuccessDialogOpen(true);
      } else {
        toast.success('Customer created successfully');
        navigate(`/customers/${data.custId}`);
      }
    },
    onError: (error: Error) => {
      toast.error(`Failed to create customer: ${error.message}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: CustomerRequestDTO) => updateCustomer(Number(id), data),
    onSuccess: () => {
      // Invalidate customer queries to ensure all customer lists refresh
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      queryClient.invalidateQueries({ queryKey: ['customer', id] });
      setSuccessDialogOpen(true);
    },
    onError: (error: Error) => {
      toast.error(`Failed to update customer: ${error.message}`);
    }
  });

  const isLoading = createMutation.isPending || updateMutation.isPending;

  // Submit handler
  const onSubmit = (data: CustomerRequestDTO) => {
    if (isEdit) {
      updateMutation.mutate(data);
    } else {
      createMutation.mutate(data);
    }
  };

  // Handle dialog close - stay on the same page for create mode, navigate for edit mode
  const handleCloseSuccessDialog = () => {
    setSuccessDialogOpen(false);
    if (!isEdit) {
      // For create mode, stay on the same page and reset form
      setValue('extCustId', '');
      setValue('firstName', '');
      setValue('lastName', '');
      setValue('tradeName', '');
      setValue('address1', '');
      setValue('mobile', '');
      setValue('custType', CustomerType.INDIVIDUAL);
    } else {
      // For edit mode, navigate back to customers list
      navigate('/customers');
    }
  };
  
  return (
    <Box>
      <PageHeader
        title={isEdit ? 'Edit Customer' : 'Add Customer'}
        buttonText="Back to List"
        buttonLink="/customers"
        startIcon={<ArrowBackIcon />}
      />

      {/* Success Dialog */}
      <Dialog
        open={successDialogOpen}
        onClose={handleCloseSuccessDialog}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">
          {isEdit ? 'Customer Updated' : 'Customer Created Successfully'}
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            {isEdit ? 'Customer updated successfully' : createdCustomerId || 'Customer created successfully'}
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseSuccessDialog} color="primary" autoFocus>
            OK
          </Button>
        </DialogActions>
      </Dialog>

      {isEdit && isLoadingCustomer ? (
        <Box display="flex" justifyContent="center" my={4}>
          <CircularProgress />
        </Box>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)}>
          <FormSection title="Customer Information">
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                {/* Customer ID field - disabled in create mode, visible in edit mode */}
                {isEdit && (
                  <TextField
                    label="Primary Cust Id"
                    value={customerData?.custId || ''}
                    fullWidth
                    disabled={true}
                    InputProps={{
                      readOnly: true,
                    }}
                  />
                )}
                {!isEdit && (
                  <TextField
                    label="Primary Cust Id"
                    value="Will be generated"
                    fullWidth
                    disabled={true}
                    InputProps={{
                      readOnly: true,
                    }}
                  />
                )}
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Controller
                  name="extCustId"
                  control={control}
                  rules={{ 
                    maxLength: {
                      value: 20,
                      message: 'External Customer ID cannot exceed 20 characters'
                    }
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="External Customer ID"
                      fullWidth
                      error={!!errors.extCustId}
                      helperText={errors.extCustId?.message}
                      disabled={isLoading}
                    />
                  )}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Controller
                  name="custType"
                  control={control}
                  rules={{ required: 'Customer Type is mandatory' }}
                  render={({ field }) => (
                    <FormControl fullWidth error={!!errors.custType}>
                      <InputLabel id="customer-type-label">Customer Type</InputLabel>
                      <Select
                        {...field}
                        labelId="customer-type-label"
                        label="Customer Type"
                        disabled={isLoading}
                      >
                        <MenuItem value={CustomerType.INDIVIDUAL}>Individual</MenuItem>
                        <MenuItem value={CustomerType.CORPORATE}>Corporate</MenuItem>
                        <MenuItem value={CustomerType.BANK}>Bank</MenuItem>
                        
                      </Select>
                      <FormHelperText>{errors.custType?.message}</FormHelperText>
                    </FormControl>
                  )}
                />
              </Grid>

              {/* Conditional fields based on customer type */}
              {custType === CustomerType.INDIVIDUAL ? (
                <>
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="firstName"
                      control={control}
                      rules={{
                        required: custType === CustomerType.INDIVIDUAL ? 'First Name is required for Individual customers' : false,
                        maxLength: {
                          value: 50,
                          message: 'First Name cannot exceed 50 characters'
                        }
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="First Name"
                          fullWidth
                          required={custType === CustomerType.INDIVIDUAL}
                          error={!!errors.firstName}
                          helperText={errors.firstName?.message}
                          disabled={isLoading}
                        />
                      )}
                    />
                  </Grid>
                  
                  <Grid item xs={12} md={6}>
                    <Controller
                      name="lastName"
                      control={control}
                      rules={{
                        required: custType === CustomerType.INDIVIDUAL ? 'Last Name is required for Individual customers' : false,
                        maxLength: {
                          value: 50,
                          message: 'Last Name cannot exceed 50 characters'
                        }
                      }}
                      render={({ field }) => (
                        <TextField
                          {...field}
                          label="Last Name"
                          fullWidth
                          required={custType === CustomerType.INDIVIDUAL}
                          error={!!errors.lastName}
                          helperText={errors.lastName?.message}
                          disabled={isLoading}
                        />
                      )}
                    />
                  </Grid>
                </>
              ) : (
                <Grid item xs={12} md={12}>
                  <Controller
                    name="tradeName"
                    control={control}
                    rules={{
                      required: (custType === CustomerType.CORPORATE || custType === CustomerType.BANK) ? 'Trade Name is required for Corporate and Bank customers' : false,
                      maxLength: {
                        value: 100,
                        message: 'Trade Name cannot exceed 100 characters'
                      }
                    }}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Trade Name"
                        fullWidth
                        required={(custType === CustomerType.CORPORATE || custType === CustomerType.BANK)}
                        error={!!errors.tradeName}
                        helperText={errors.tradeName?.message}
                        disabled={isLoading}
                      />
                    )}
                  />
                </Grid>
              )}

              <Grid item xs={12} md={6}>
                <Controller
                  name="address1"
                  control={control}
                  rules={{
                    maxLength: {
                      value: 200,
                      message: 'Address cannot exceed 200 characters'
                    }
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Address"
                      fullWidth
                      error={!!errors.address1}
                      helperText={errors.address1?.message}
                      disabled={isLoading}
                    />
                  )}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Controller
                  name="mobile"
                  control={control}
                  rules={{
                    required: 'Mobile number is required',
                    pattern: {
                      value: /^01[0-9]{9}$/,
                      message: 'Please enter a valid Bangladeshi mobile number'
                    },
                    validate: (value) => {
                      if (!value) return 'Mobile number is required';
                      if (!/^01[0-9]{9}$/.test(value)) {
                        return 'Please enter a valid Bangladeshi mobile number';
                      }
                      return true;
                    }
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Mobile Number"
                      fullWidth
                      required
                      error={!!errors.mobile}
                      helperText={errors.mobile?.message || "Format: 01XXXXXXXXX (11 digits starting with 01)"}
                      disabled={isLoading}
                      placeholder="01712345678"
                    />
                  )}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Controller
                  name="makerId"
                  control={control}
                  rules={{
                    required: 'Maker ID is mandatory',
                    maxLength: {
                      value: 20,
                      message: 'Maker ID cannot exceed 20 characters'
                    }
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Maker ID"
                      fullWidth
                      required
                      error={!!errors.makerId}
                      helperText={errors.makerId?.message}
                      disabled={isLoading}
                    />
                  )}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <TextField
                  label="Branch Code"
                  value="001"
                  fullWidth
                  disabled={true}
                  InputProps={{
                    readOnly: true,
                  }}
                />
              </Grid>
            </Grid>
          </FormSection>

          <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
            <Button
              component={RouterLink}
              to="/customers"
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
              {isEdit ? 'Update' : 'Create'} Customer
            </Button>
          </Box>
        </form>
      )}
    </Box>
  );
};

export default CustomerForm;
