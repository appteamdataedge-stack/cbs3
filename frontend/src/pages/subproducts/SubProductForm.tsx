import { ArrowBack as ArrowBackIcon, Save as SaveIcon } from '@mui/icons-material';
import {
  Box,
  Button,
  CircularProgress,
  FormControl,
  FormHelperText,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  TextField
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAllProducts } from '../../api/productService';
import { createSubProduct, getSubProductById, updateSubProduct, getSubProductGLOptions, getSubProductGLOptionsByParent } from '../../api/subProductService';
import { getAllInterestRates } from '../../api/interestRateService';
import { getInterestPayableReceivableLayer4GLs, getInterestIncomeExpenditureLayer4GLs } from '../../api/glSetupService';
import { FormSection, PageHeader } from '../../components/common';
import type { SubProductRequestDTO } from '../../types';
import { SubProductStatus } from '../../types';

const SubProductForm = () => {
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
  } = useForm<SubProductRequestDTO>({
    defaultValues: {
      productId: undefined, // Don't set to 0, let it be undefined initially
      subProductCode: '',
      subProductName: '',
      inttCode: '',
      cumGLNum: '',
      extGLNum: '',
      subProductStatus: SubProductStatus.ACTIVE,
      makerId: 'FRONTEND_USER', // Default maker ID
    }
  });

  // Watch productId to filter GL options
  const selectedProductId = watch('productId');
  
  // (moved below productsData query)

  // Get subproduct data if editing
  const { data: subProductData, isLoading: isLoadingSubProduct } = useQuery({
    queryKey: ['subProduct', id],
    queryFn: () => getSubProductById(Number(id)),
    enabled: isEdit,
  });

  // Get products for dropdown
  const { data: productsData, isLoading: isLoadingProducts } = useQuery({
    queryKey: ['products', { page: 0, size: 100 }], // Get all products for dropdown
    queryFn: () => getAllProducts(0, 100),
  });

  // Get interest rates for dropdown
  const { data: interestRatesData, isLoading: isLoadingInterestRates } = useQuery({
    queryKey: ['interest-rates'],
    queryFn: () => getAllInterestRates(),
  });

  // Get selected product to check if it's customer+interest bearing (must be after productsData is defined)
  const selectedProduct = productsData?.content?.find(p => p.productId === selectedProductId);
  const isInterestBearingProduct = Boolean(selectedProduct?.customerProductFlag && selectedProduct?.interestBearingFlag);


  // Get Layer 4 GL options for sub-product dropdown
  const { isLoading: isLoadingGLSetups } = useQuery({
    queryKey: ['subproduct-gl-options'],
    queryFn: () => getSubProductGLOptions(),
  });

  // Get Layer 4 GL options filtered by parent GL number
  const { data: filteredGLSetups, isLoading: isLoadingFilteredGLSetups } = useQuery({
    queryKey: ['subproduct-gl-options', selectedProduct?.cumGLNum],
    queryFn: () => getSubProductGLOptionsByParent(selectedProduct?.cumGLNum || ''),
    enabled: !!selectedProduct?.cumGLNum,
  });

  // Get Layer 4 GL options for interest payable/receivable accounts
  const { data: interestPayableReceivableLayer4GLs, isLoading: isLoadingInterestPayableReceivable } = useQuery({
    queryKey: ['interest-payable-receivable-layer4'],
    queryFn: () => getInterestPayableReceivableLayer4GLs(),
  });

  // Get Layer 4 GL options for interest income/expenditure accounts
  const { data: interestIncomeExpenditureLayer4GLs, isLoading: isLoadingInterestIncomeExpenditure } = useQuery({
    queryKey: ['interest-income-expenditure-layer4'],
    queryFn: () => getInterestIncomeExpenditureLayer4GLs(),
  });

  // Set form values when subproduct data is loaded
  useEffect(() => {
    if (subProductData && isEdit) {
      // Set form values from loaded subproduct data (matching backend field order)
      setValue('productId', subProductData.productId);
      setValue('subProductCode', subProductData.subProductCode);
      setValue('subProductName', subProductData.subProductName);
      setValue('inttCode', subProductData.inttCode);
      setValue('cumGLNum', subProductData.cumGLNum);
      setValue('extGLNum', subProductData.extGLNum);
      setValue('subProductStatus', subProductData.subProductStatus);
      setValue('makerId', 'FRONTEND_USER'); // Use default for edits too
      
      // Set interest fields if they exist
      if ((subProductData as any).interestIncrement !== undefined) {
        setValue('interestIncrement' as any, (subProductData as any).interestIncrement);
      }
      if ((subProductData as any).interestReceivableExpenditureGLNum) {
        setValue('interestReceivableExpenditureGLNum' as any, (subProductData as any).interestReceivableExpenditureGLNum);
      }
      if ((subProductData as any).interestIncomePayableGLNum) {
        setValue('interestIncomePayableGLNum' as any, (subProductData as any).interestIncomePayableGLNum);
      }
    }
  }, [subProductData, isEdit, setValue]);

  // Mutations for create and update
  const createMutation = useMutation({
    mutationFn: createSubProduct,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['subproducts'] });
      toast.success(`SubProduct created successfully! SubProduct ID: ${data.subProductId}`);
      // Reset form to allow creating another subproduct
      reset();
      // Stay on the same page - don't navigate away
    },
    onError: (error: Error) => {
      console.error('Create sub-product error:', error);
      toast.error(`Failed to create subproduct: ${error.message}`);
    }
  });

  const updateMutation = useMutation({
    mutationFn: (data: SubProductRequestDTO) => updateSubProduct(Number(id), data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subproducts'] });
      queryClient.invalidateQueries({ queryKey: ['subProduct', id] });
      toast.success('SubProduct updated successfully');
      navigate('/subproducts');
    },
    onError: (error: Error) => {
      toast.error(`Failed to update subproduct: ${error.message}`);
    }
  });

  const isLoading = createMutation.isPending || updateMutation.isPending || isLoadingSubProduct || isLoadingProducts || isLoadingInterestRates || isLoadingGLSetups || isLoadingFilteredGLSetups;
  const isDisabled = isLoading;

  // Clear GL fields when product changes
  useEffect(() => {
    if (selectedProductId && !isEdit) {
      setValue('cumGLNum', '');
      setValue('inttCode', '');
      setValue('extGLNum', '');
    }
  }, [selectedProductId, isEdit, setValue]);

  // Submit handler
  const onSubmit = (data: SubProductRequestDTO) => {
    // Validate required fields before submission
    if (!data.productId || data.productId === 0) {
      alert('Please select a product');
      return;
    }
    
    if (!data.cumGLNum || data.cumGLNum.trim() === '') {
      alert('Please select a GL Number');
      return;
    }
    
    // Validate interest fields only for customer+interest bearing products
    if (isInterestBearingProduct) {
      if (!data.inttCode || data.inttCode.trim() === '') {
        alert('Please enter an Interest Code for interest-bearing products');
        return;
      }
      
      if (data.interestIncrement === undefined || data.interestIncrement === null || isNaN(data.interestIncrement as any)) {
        alert('Please enter Interest Increment for interest-bearing products');
        return;
      }
      
      // At least one GL field must be configured
      const hasReceivableExp = data.interestReceivableExpenditureGLNum && data.interestReceivableExpenditureGLNum.trim() !== '';
      const hasIncomePay = data.interestIncomePayableGLNum && data.interestIncomePayableGLNum.trim() !== '';

      if (!hasReceivableExp && !hasIncomePay) {
        alert('Please enter at least one interest GL field for interest-bearing products');
        return;
      }
    }
    
    // External GL Number is optional for all products (no validation required)
    
    // Sanitize data - convert empty strings to null/undefined for optional fields
    const sanitizedData = {
      ...data,
      // Convert interestIncrement to number if it's a string, or keep it if it's already a number
      interestIncrement: isInterestBearingProduct
        ? (typeof data.interestIncrement === 'string' ? parseFloat(data.interestIncrement) : data.interestIncrement)
        : undefined,
      interestReceivableExpenditureGLNum: data.interestReceivableExpenditureGLNum?.trim() || undefined,
      interestIncomePayableGLNum: data.interestIncomePayableGLNum?.trim() || undefined,
    };
    
    console.log('Original data:', data);
    console.log('Sanitized data:', sanitizedData);
    console.log('Is interest bearing product:', isInterestBearingProduct);
    
    if (isEdit) {
      updateMutation.mutate(sanitizedData as any);
    } else {
      createMutation.mutate(sanitizedData as any);
    }
  };

  return (
    <Box>
      <PageHeader
        title={isEdit ? 'Edit SubProduct' : 'Add SubProduct'}
        buttonText="Back to List"
        buttonLink="/subproducts"
        startIcon={<ArrowBackIcon />}
      />

      {isEdit && isLoadingSubProduct ? (
        <Box display="flex" justifyContent="center" my={4}>
          <CircularProgress />
        </Box>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)}>
          <FormSection title="SubProduct Information">
            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Controller
                  name="subProductCode"
                  control={control}
                  rules={{ 
                    required: 'SubProduct Code is mandatory'
                  }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="SubProduct Code"
                      fullWidth
                      required
                      error={!!errors.subProductCode}
                      helperText={errors.subProductCode?.message}
                      disabled={isDisabled}
                    />
                  )}
                />
              </Grid>
              
              <Grid item xs={12} md={6}>
                <Controller
                  name="subProductName"
                  control={control}
                  rules={{ required: 'SubProduct Name is mandatory' }}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      label="SubProduct Name"
                      fullWidth
                      required
                      error={!!errors.subProductName}
                      helperText={errors.subProductName?.message}
                      disabled={isDisabled}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <Controller
                  name="productId"
                  control={control}
                  rules={{ required: 'Product is mandatory' }}
                  render={({ field }) => (
                    <FormControl fullWidth error={!!errors.productId} disabled={isDisabled}>
                      <InputLabel id="product-label">Product</InputLabel>
                      <Select
                        {...field}
                        labelId="product-label"
                        label="Product"
                      >
                        {productsData?.content.map((product) => (
                          <MenuItem key={product.productId} value={product.productId}>
                            {product.productName} ({product.productCode})
                          </MenuItem>
                        ))}
                      </Select>
                      <FormHelperText>{errors.productId?.message}</FormHelperText>
                    </FormControl>
                  )}
                />
              </Grid>


              <Grid item xs={12} md={6}>
                <Controller
                  name="cumGLNum"
                  control={control}
                  rules={{ required: 'Cumulative GL Number is mandatory' }}
                  render={({ field }) => (
                    <FormControl fullWidth error={!!errors.cumGLNum}>
                      <InputLabel id="cum-gl-number-label">Cumulative GL Number</InputLabel>
                      <Select
                        {...field}
                        labelId="cum-gl-number-label"
                        label="Cumulative GL Number"
                        disabled={isDisabled || !selectedProduct?.cumGLNum}
                      >
                        {(filteredGLSetups || []).map((glSetup) => (
                          <MenuItem key={glSetup.glNum} value={glSetup.glNum}>
                            {glSetup.glName} - {glSetup.glNum}
                          </MenuItem>
                        ))}
                      </Select>
                      <FormHelperText>
                        {errors.cumGLNum?.message || 
                         (!selectedProduct?.cumGLNum ? 'Please select a product first' : '')}
                      </FormHelperText>
                    </FormControl>
                  )}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <Controller
                  name="inttCode"
                  control={control}
                  rules={{ 
                    required: isInterestBearingProduct ? 'Interest Code is mandatory for interest-bearing products' : false 
                  }}
                  render={({ field }) => (
                    <FormControl fullWidth error={!!errors.inttCode} disabled={isDisabled}>
                      <InputLabel id="intt-code-label">Interest Code</InputLabel>
                      <Select
                        {...field}
                        labelId="intt-code-label"
                        label="Interest Code"
                      >
                        {interestRatesData?.map((rate) => (
                          <MenuItem key={rate.inttCode} value={rate.inttCode}>
                            {rate.inttCode} ({rate.inttRate}%)
                          </MenuItem>
                        ))}
                      </Select>
                      <FormHelperText>
                        {errors.inttCode?.message || 
                         (isInterestBearingProduct ? "Select interest code for this sub-product" : "Interest code (only for interest-bearing products)")}
                      </FormHelperText>
                    </FormControl>
                  )}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <Controller
                  name="extGLNum"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      fullWidth
                      label="External GL Number"
                      error={!!errors.extGLNum}
                      helperText={errors.extGLNum?.message || "External GL Number for this sub-product (optional)"}
                      disabled={isDisabled}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} md={6}>
                <Controller
                  name="subProductStatus"
                  control={control}
                  rules={{ required: 'Status is mandatory' }}
                  render={({ field }) => (
                    <FormControl fullWidth error={!!errors.subProductStatus}>
                      <InputLabel id="status-label">Status</InputLabel>
                      <Select
                        {...field}
                        labelId="status-label"
                        label="Status"
                        disabled={isDisabled}
                      >
                        <MenuItem value={SubProductStatus.ACTIVE}>Active</MenuItem>
                        <MenuItem value={SubProductStatus.INACTIVE}>Inactive</MenuItem>
                        <MenuItem value={SubProductStatus.DEACTIVE}>Deactive</MenuItem>
                      </Select>
                      <FormHelperText>{errors.subProductStatus?.message}</FormHelperText>
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

          {/* Interest Configuration Section - Only show for customer+interest bearing products */}
          {isInterestBearingProduct && (
            <FormSection title="Interest Configuration">
              <Grid container spacing={3}>
                <Grid item xs={12} md={6}>
                  <Controller
                    name={"interestIncrement" as any}
                    control={control}
                    rules={{
                      required: isInterestBearingProduct ? 'Interest Increment is mandatory for interest-bearing products' : false
                    }}
                    render={({ field: { onChange, value, ...field } }) => (
                      <TextField
                        {...field}
                        value={value ?? ''}
                        onChange={(e) => {
                          const val = e.target.value;
                          onChange(val === '' ? undefined : parseFloat(val));
                        }}
                        label="Interest Increment (%)"
                        type="number"
                        fullWidth
                        required={isInterestBearingProduct}
                        error={!!(errors as any).interestIncrement}
                        helperText={(errors as any).interestIncrement?.message || "Additional interest rate over base rate (can be negative/zero/positive)"}
                        disabled={isDisabled}
                        inputProps={{ step: 0.01 }}
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <TextField
                    label="Effective Interest Rate (%)"
                    type="number"
                    fullWidth
                    disabled={true}
                    value={(watch("interestIncrement") || 0).toFixed(4)}
                    helperText="Base Rate from Interest Rate Master + Interest Increment (read-only)"
                    InputProps={{
                      readOnly: true,
                    }}
                  />
                </Grid>

                {/* Layer 4 GL Selection for Interest Receivable/Payable (Consolidated) */}
                <Grid item xs={12} md={6}>
                  <Controller
                    name={"interestIncomePayableGLNum" as any}
                    control={control}
                    render={({ field }) => (
                      <FormControl fullWidth error={!!(errors as any).interestIncomePayableGLNum}>
                        <InputLabel id="interest-receivable-payable-gl-label">Interest Payable/Income GL</InputLabel>
                        <Select
                          {...field}
                          labelId="interest-receivable-payable-gl-label"
                          label="Interest Payable/Income GL"
                          disabled={isDisabled || isLoadingInterestPayableReceivable}
                        >
                          {interestPayableReceivableLayer4GLs?.map((gl) => (
                            <MenuItem key={gl.glNum} value={gl.glNum}>
                              {gl.glNum} - {gl.glName}
                            </MenuItem>
                          ))}
                        </Select>
                        <FormHelperText>
                          {(errors as any).interestIncomePayableGLNum?.message ||
                           (isLoadingInterestPayableReceivable ? "Loading GL options..." :
                            "Payable and Income for liabilities (GL starting with 13, 14)")}
                        </FormHelperText>
                      </FormControl>
                    )}
                  />
                </Grid>

                {/* Layer 4 GL Selection for Interest Income/Expenditure (Consolidated) */}
                <Grid item xs={12} md={6}>
                  <Controller
                    name={"interestReceivableExpenditureGLNum" as any}
                    control={control}
                    render={({ field }) => (
                      <FormControl fullWidth error={!!(errors as any).interestReceivableExpenditureGLNum}>
                        <InputLabel id="interest-income-expenditure-gl-label">Interest Expenditure/Receivable GL</InputLabel>
                        <Select
                          {...field}
                          labelId="interest-income-expenditure-gl-label"
                          label="Interest Expenditure/Receivable GL"
                          disabled={isDisabled || isLoadingInterestIncomeExpenditure}
                        >
                          {interestIncomeExpenditureLayer4GLs?.map((gl) => (
                            <MenuItem key={gl.glNum} value={gl.glNum}>
                              {gl.glNum} - {gl.glName}
                            </MenuItem>
                          ))}
                        </Select>
                        <FormHelperText>
                          {(errors as any).interestReceivableExpenditureGLNum?.message ||
                           (isLoadingInterestIncomeExpenditure ? "Loading GL options..." :
                            "Expenditure and Receivable for assets (GL starting with 23, 24)")}
                        </FormHelperText>
                      </FormControl>
                    )}
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
              to="/subproducts"
              variant="outlined"
              disabled={isLoading}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={isDisabled}
              startIcon={isLoading ? <CircularProgress size={20} /> : <SaveIcon />}
            >
              {isEdit ? 'Update' : 'Create'} SubProduct
            </Button>
          </Box>
        </form>
      )}
    </Box>
  );
};

export default SubProductForm;
