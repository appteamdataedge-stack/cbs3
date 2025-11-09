import { ArrowBack as ArrowBackIcon, Save as SaveIcon } from '@mui/icons-material';
import {
  Box,
  Button,
  CircularProgress,
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
import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { createProduct, getProductById, updateProduct, getProductGLOptions } from '../../api/productService';
import { FormSection, PageHeader } from '../../components/common';
import type { ProductRequestDTO } from '../../types';

const ProductForm = () => {
  const { id } = useParams<{ id: string }>();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  // Form setup with react-hook-form
  const {
    control,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors }
  } = useForm<ProductRequestDTO>({
    defaultValues: {
      productCode: '',
      productName: '',
      cumGLNum: '', // GL Number field
      customerProductFlag: false,
      interestBearingFlag: false,
      dealOrRunning: '',
      currency: 'BDT',
      makerId: 'FRONTEND_USER', // Default maker ID
    }
  });

  // Watch customerProductFlag to conditionally show interestBearingFlag and dealOrRunning
  const isCustomerProduct = watch('customerProductFlag');
  const isInterestBearing = watch('interestBearingFlag');

  // Get product data if editing
  const { data: productData, isLoading: isLoadingProduct } = useQuery({
    queryKey: ['product', id],
    queryFn: () => getProductById(Number(id)),
    enabled: isEdit,
  });

  // Get GL setups for layer 3
  const { data: glSetups, isLoading: isLoadingGLSetups } = useQuery({
    queryKey: ['product-gl-options'],
    queryFn: () => getProductGLOptions(),
  });

  // Set form values when product data is loaded
  useEffect(() => {
    if (productData && isEdit) {
      // Set form values from loaded product data
      setValue('productCode', productData.productCode);
      setValue('productName', productData.productName);
      setValue('cumGLNum', productData.cumGLNum);
      setValue('customerProductFlag', productData.customerProductFlag || false);
      setValue('interestBearingFlag', productData.interestBearingFlag || false);
      setValue('dealOrRunning', productData.dealOrRunning || '');
      setValue('currency', productData.currency || 'BDT');
      setValue('makerId', 'FRONTEND_USER'); // Use default for edits too
    }
  }, [productData, isEdit, setValue]);

  // Reset interestBearingFlag when customerProductFlag is turned off
  useEffect(() => {
    if (!isCustomerProduct) {
      setValue('interestBearingFlag', false);
      setValue('dealOrRunning', '');
    }
  }, [isCustomerProduct, setValue]);

  // Mutations for create and update
  const createMutation = useMutation({
    mutationFn: createProduct,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      toast.success(`Product created successfully! Product ID: ${data.productId}`);
      // Reset form to allow creating another product
      reset();
      // Stay on the same page - don't navigate away
    },
    onError: (error: Error) => {
      toast.error(`Failed to create product: ${error.message}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: ProductRequestDTO) => updateProduct(Number(id), data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['products'] });
      queryClient.invalidateQueries({ queryKey: ['product', id] });
      toast.success('Product updated successfully');
      navigate('/products');
    },
    onError: (error: Error) => {
      toast.error(`Failed to update product: ${error.message}`);
    }
  });

  const isLoading = createMutation.isPending || updateMutation.isPending;

  // Submit handler
  const onSubmit = (data: ProductRequestDTO) => {
    if (isEdit) {
      // Preserve the original product code when editing
      const updateData = {
        ...data,
        productCode: productData?.productCode || data.productCode
      };
      updateMutation.mutate(updateData);
    } else {
      createMutation.mutate(data);
    }
  };

  return (
    <Box>
      <PageHeader
        title={isEdit ? 'Edit Product' : 'Add Product'}
        buttonText="Back to List"
        buttonLink="/products"
        startIcon={<ArrowBackIcon />}
      />

      {isEdit && isLoadingProduct ? (
        <Box display="flex" justifyContent="center" my={4}>
          <CircularProgress />
        </Box>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)}>
          <FormSection title="Product Information">
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Controller
                  name="productCode"
                  control={control}
                  rules={{ 
                    required: 'Product Code is mandatory'
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Product Code"
                      fullWidth
                      required
                      error={!!errors.productCode}
                      helperText={errors.productCode?.message}
                      disabled={isLoading || isEdit}
                      InputProps={isEdit ? { readOnly: true } : undefined}
                    />
                  )}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Box display="flex" alignItems="center" gap={2}>
                  <Controller
                    name="customerProductFlag"
                    control={control}
                    render={({ field }) => (
                      <FormControlLabel
                        control={
                          <Switch
                            {...field}
                            checked={field.value || false}
                            disabled={isLoading}
                          />
                        }
                        label="Customer Product"
                      />
                    )}
                  />
                  
                  {isCustomerProduct && (
                    <Controller
                      name="interestBearingFlag"
                      control={control}
                      render={({ field }) => (
                        <FormControlLabel
                          control={
                            <Switch
                              {...field}
                              checked={field.value || false}
                              disabled={isLoading}
                            />
                          }
                          label="Interest Bearing"
                        />
                      )}
                    />
                  )}
                </Box>
              </Grid>

              <Grid item xs={12} md={6}>
                <Controller
                  name="cumGLNum"
                  control={control}
                  rules={{ required: 'GL Number is mandatory' }}
                  render={({ field }) => (
                    <FormControl fullWidth error={!!errors.cumGLNum}>
                      <InputLabel id="gl-number-label">GL Number</InputLabel>
                      <Select
                        {...field}
                        labelId="gl-number-label"
                        label="GL Number"
                        disabled={isLoading || isLoadingGLSetups}
                      >
                        {glSetups?.map((glSetup) => (
                          <MenuItem key={glSetup.glNum} value={glSetup.glNum}>
                            {glSetup.glName} - {glSetup.glNum}
                          </MenuItem>
                        ))}
                      </Select>
                      <FormHelperText>{errors.cumGLNum?.message}</FormHelperText>
                    </FormControl>
                  )}
                />
              </Grid>

              <Grid item xs={12}>
                <Controller
                  name="productName"
                  control={control}
                  rules={{ required: 'Product Name is mandatory' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="Product Name"
                      fullWidth
                      required
                      error={!!errors.productName}
                      helperText={errors.productName?.message}
                      disabled={isLoading}
                    />
                  )}
                />
              </Grid>
              
              {/* Deal or Running field - only show when Customer Product = Y AND Interest Bearing = Y */}
              {isCustomerProduct && isInterestBearing && (
                <Grid item xs={12} md={6}>
                  <Controller
                    name="dealOrRunning"
                    control={control}
                    rules={{ required: 'Deal or Running is mandatory' }}
                    render={({ field }) => (
                      <FormControl fullWidth error={!!errors.dealOrRunning}>
                        <InputLabel id="deal-or-running-label">Deal or Running *</InputLabel>
                        <Select
                          {...field}
                          labelId="deal-or-running-label"
                          label="Deal or Running *"
                          disabled={isLoading}
                        >
                          <MenuItem value="Deal">Deal</MenuItem>
                          <MenuItem value="Running">Running</MenuItem>
                        </Select>
                        <FormHelperText>{errors.dealOrRunning?.message}</FormHelperText>
                      </FormControl>
                    )}
                  />
                </Grid>
              )}

              {/* Currency field */}
              <Grid item xs={12} md={6}>
                <Controller
                  name="currency"
                  control={control}
                  rules={{ required: 'Currency is mandatory' }}
                  render={({ field }) => (
                    <FormControl fullWidth error={!!errors.currency}>
                      <InputLabel id="currency-label">Currency</InputLabel>
                      <Select
                        {...field}
                        labelId="currency-label"
                        label="Currency"
                        disabled={isLoading}
                      >
                        <MenuItem value="BDT">BDT</MenuItem>
                        <MenuItem value="USD">USD</MenuItem>
                        <MenuItem value="GBP">GBP</MenuItem>
                        <MenuItem value="EUR">EUR</MenuItem>
                      </Select>
                      <FormHelperText>{errors.currency?.message}</FormHelperText>
                    </FormControl>
                  )}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Controller
                  name="makerId"
                  control={control}
                  rules={{
                    required: 'Maker ID is mandatory'
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
            </Grid>
          </FormSection>

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
              to="/products"
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
              {isEdit ? 'Update' : 'Create'} Product
            </Button>
          </Box>
        </form>
      )}
    </Box>
  );
};

export default ProductForm;
