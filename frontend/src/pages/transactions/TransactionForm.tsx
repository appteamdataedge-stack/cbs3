import { Add as AddIcon, ArrowBack as ArrowBackIcon, Delete as DeleteIcon, Save as SaveIcon } from '@mui/icons-material';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  FormControl,
  FormHelperText,
  Grid,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState, useMemo } from 'react';
import { Controller, useFieldArray, useForm, useWatch } from 'react-hook-form';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAllCustomerAccounts } from '../../api/customerAccountService';
import { getAllOfficeAccounts } from '../../api/officeAccountService';
import { createTransaction, getAccountBalance, getAccountOverdraftStatus } from '../../api/transactionService';
import { FormSection, PageHeader } from '../../components/common';
import type { CombinedAccountDTO, TransactionRequestDTO, AccountBalanceDTO } from '../../types';
import { DrCrFlag } from '../../types';

// Available currencies
const CURRENCIES = ['BDT', 'USD', 'EUR', 'GBP', 'JPY'];

const TransactionForm = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [currentDate, setCurrentDate] = useState<string>('');
  const [accountBalances, setAccountBalances] = useState<Map<string, AccountBalanceDTO>>(new Map());
  const [accountOverdraftStatus, setAccountOverdraftStatus] = useState<Map<string, boolean>>(new Map());
  const [assetAccounts, setAssetAccounts] = useState<Map<string, boolean>>(new Map());
  const [loadingBalances, setLoadingBalances] = useState<Set<number>>(new Set());

  // Fetch customer accounts for dropdown
  const { data: customerAccountsData, isLoading: isLoadingCustomerAccounts } = useQuery({
    queryKey: ['customer-accounts', { page: 0, size: 100 }], // Get all customer accounts for dropdown
    queryFn: () => getAllCustomerAccounts(0, 100),
  });

  // Fetch office accounts for dropdown
  const { data: officeAccountsData, isLoading: isLoadingOfficeAccounts } = useQuery({
    queryKey: ['office-accounts', { page: 0, size: 100 }], // Get all office accounts for dropdown
    queryFn: () => getAllOfficeAccounts(0, 100),
  });

  // Combine customer and office accounts into a unified list
  const allAccounts: CombinedAccountDTO[] = useMemo(() => {
    const customerAccounts: CombinedAccountDTO[] = customerAccountsData?.content?.map(account => ({
      ...account,
      accountType: 'Customer' as const,
      displayName: `${account.acctName} (${account.accountNo}) - Customer`
    })) || [];
    
    const officeAccounts: CombinedAccountDTO[] = officeAccountsData?.content?.map(account => ({
      ...account,
      accountType: 'Office' as const,
      displayName: `${account.acctName} (${account.accountNo}) - Office`
    })) || [];
    
    return [...customerAccounts, ...officeAccounts];
  }, [customerAccountsData, officeAccountsData]);

  const isLoadingAccounts = isLoadingCustomerAccounts || isLoadingOfficeAccounts;

  // Set current date on component mount
  useEffect(() => {
    const today = new Date().toISOString().split('T')[0];
    setCurrentDate(today);
  }, []);

  // Form setup with react-hook-form
  const { 
    control, 
    handleSubmit, 
    setValue,
    watch,
    formState: { errors }
  } = useForm<TransactionRequestDTO>({
    defaultValues: {
      valueDate: currentDate || new Date().toISOString().split('T')[0],
      narration: '',
      lines: [
        { accountNo: '', drCrFlag: DrCrFlag.D, tranCcy: 'BDT', fcyAmt: 0, exchangeRate: 1, lcyAmt: 0, udf1: '' },
        { accountNo: '', drCrFlag: DrCrFlag.C, tranCcy: 'BDT', fcyAmt: 0, exchangeRate: 1, lcyAmt: 0, udf1: '' }
      ]
    }
  });

  // Field array for transaction lines
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'lines'
  });

  // Watch all lines using useWatch for deep reactivity
  const watchedLines = useWatch({
    control,
    name: 'lines'
  });

  // Calculate totals dynamically - useMemo ensures instant updates
  const { totalDebit, totalCredit, isBalanced } = useMemo(() => {
    let debitTotal = 0;
    let creditTotal = 0;

    // Use watchedLines which updates on every field change
    const linesToCalculate = watchedLines || [];
    
    linesToCalculate.forEach((line: any) => {
      // Parse the amount value, handling null, undefined, empty string, and NaN
      const amount = parseFloat(String(line?.lcyAmt || 0));
      const validAmount = isNaN(amount) ? 0 : amount;
      
      if (line?.drCrFlag === DrCrFlag.D) {
        debitTotal += validAmount;
      } else if (line?.drCrFlag === DrCrFlag.C) {
        creditTotal += validAmount;
      }
    });

    // Round to 2 decimal places for display
    debitTotal = Math.round(debitTotal * 100) / 100;
    creditTotal = Math.round(creditTotal * 100) / 100;

    const balanced = Math.abs(debitTotal - creditTotal) < 0.01;

    // Log for debugging
    console.log('Totals calculated:', { debitTotal, creditTotal, balanced });

    return {
      totalDebit: debitTotal,
      totalCredit: creditTotal,
      isBalanced: balanced
    };
  }, [watchedLines]);

  // Set current date when available
  useEffect(() => {
    if (currentDate) {
      setValue('valueDate', currentDate);
    }
  }, [currentDate, setValue]);

  // Fetch account balance and overdraft status when account is selected
  const fetchAccountBalance = async (accountNo: string, index: number) => {
    if (!accountNo) return;
    
    try {
      setLoadingBalances(prev => new Set(prev).add(index));
      
      // Find the selected account to check if it's an Asset Account (both Customer and Office)
      const selectedAccount = allAccounts.find(acc => acc.accountNo === accountNo);
      const isAssetAccount = selectedAccount?.glNum?.startsWith('2');
      
      // Fetch both balance and overdraft status in parallel
      const [balanceData, overdraftData] = await Promise.all([
        getAccountBalance(accountNo),
        getAccountOverdraftStatus(accountNo)
      ]);
      
      setAccountBalances(prev => new Map(prev).set(`${index}`, balanceData));
      setAccountOverdraftStatus(prev => new Map(prev).set(`${index}`, overdraftData.isOverdraftAccount));
      setAssetAccounts(prev => new Map(prev).set(`${index}`, isAssetAccount || false));
      
    } catch (error) {
      console.error(`Failed to fetch data for account ${accountNo}:`, error);
      toast.error(`Failed to fetch data for account ${accountNo}`);
    } finally {
      setLoadingBalances(prev => {
        const newSet = new Set(prev);
        newSet.delete(index);
        return newSet;
      });
    }
  };

  // Create transaction mutation
  const createTransactionMutation = useMutation({
    mutationFn: createTransaction,
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['transactions'] });
      toast.success(`Transaction created successfully with status: ${data.status}. Transaction ID: ${data.tranId}`);
      toast.info('Transaction is in Entry status. It needs to be Posted by a Checker to update balances.');
      navigate('/transactions');
    },
    onError: (error: Error) => {
      toast.error(`Failed to create transaction: ${error.message}`);
    }
  });

  const isLoading = createTransactionMutation.isPending || isLoadingAccounts;

  // Add a new transaction line
  const addLine = () => {
    append({ 
      accountNo: '', 
      drCrFlag: DrCrFlag.C, 
      tranCcy: 'BDT', 
      fcyAmt: 0, 
      exchangeRate: 1, 
      lcyAmt: 0, 
      udf1: '' 
    });
  };

  // Submit handler
  const onSubmit = (data: TransactionRequestDTO) => {
    // Validate all amounts are greater than 0
    const invalidLines = data.lines.filter(line => !line.lcyAmt || line.lcyAmt <= 0);
    if (invalidLines.length > 0) {
      toast.error('All lines must have an amount greater than zero');
      return;
    }

    // Validate debit transactions don't exceed available balance 
    // (except for overdraft accounts and Asset Office Accounts)
    for (let i = 0; i < data.lines.length; i++) {
      const line = data.lines[i];
      if (line.drCrFlag === DrCrFlag.D) {
        const balance = accountBalances.get(`${i}`);
        const isOverdraftAccount = accountOverdraftStatus.get(`${i}`) || false;
        const isAssetAccount = assetAccounts.get(`${i}`) || false;
        
        // Skip balance validation for:
        // 1. Overdraft accounts (can go negative by design)
        // 2. Asset Accounts (GL starting with "2" - no validation required for negative balance)
        if (!isOverdraftAccount && !isAssetAccount && balance && line.lcyAmt > balance.computedBalance) {
          toast.error(`Insufficient balance for account ${line.accountNo}. Available: ${balance.computedBalance.toFixed(2)} BDT, Requested: ${line.lcyAmt} BDT`);
          return;
        }
      }
    }

    // Validate asset accounts cannot have positive balance
    for (let i = 0; i < data.lines.length; i++) {
      const line = data.lines[i];
      const balance = accountBalances.get(`${i}`);
      const isAssetAccount = assetAccounts.get(`${i}`) || false;
      
      // Check if this is an asset account (GL starting with '2')
      if (isAssetAccount && balance) {
        let resultingBalance = balance.computedBalance;
        
        // Calculate resulting balance after this transaction
        if (line.drCrFlag === DrCrFlag.D) {
          resultingBalance -= line.lcyAmt;
        } else {
          resultingBalance += line.lcyAmt;
        }
        
        // Asset accounts cannot have positive balance
        if (resultingBalance > 0) {
          toast.error(`Asset account ${line.accountNo} cannot have positive balance. Current: ${balance.computedBalance.toFixed(2)} BDT, Transaction: ${line.drCrFlag} ${line.lcyAmt} BDT would result in positive balance: ${resultingBalance.toFixed(2)} BDT. Asset accounts can only have zero or negative balances.`);
          return;
        }
      }
    }

    // Calculate totals manually to ensure precision
    let debitTotal = 0;
    let creditTotal = 0;
    
    data.lines.forEach(line => {
      const amount = parseFloat(String(line.lcyAmt));
      if (line.drCrFlag === DrCrFlag.D) {
        debitTotal += amount;
      } else if (line.drCrFlag === DrCrFlag.C) {
        creditTotal += amount;
      }
    });

    // Round to 2 decimal places to avoid floating point precision issues
    debitTotal = Math.round(debitTotal * 100) / 100;
    creditTotal = Math.round(creditTotal * 100) / 100;

    console.log('Transaction validation:', {
      debitTotal,
      creditTotal,
      difference: Math.abs(debitTotal - creditTotal),
      isBalanced: debitTotal === creditTotal
    });

    // Ensure debit equals credit before submitting
    if (debitTotal !== creditTotal) {
      toast.error(`Transaction is not balanced. Debit: ${debitTotal} BDT, Credit: ${creditTotal} BDT`);
      return;
    }

    // Ensure all numeric fields are properly formatted and rounded to 2 decimals
    // Set FCY and Exchange Rate to match LCY since we're using BDT only
    const formattedData = {
      ...data,
      lines: data.lines.map(line => ({
        ...line,
        fcyAmt: Math.round((Number(line.lcyAmt) || 0) * 100) / 100, // FCY = LCY for BDT
        exchangeRate: 1, // Always 1 for local currency
        lcyAmt: Math.round((Number(line.lcyAmt) || 0) * 100) / 100
      }))
    };

    // Log the data being sent for debugging
    console.log('Submitting transaction data:', JSON.stringify(formattedData, null, 2));

    createTransactionMutation.mutate(formattedData);
  };

  return (
    <Box>
      <PageHeader
        title="Create Transaction"
        buttonText="Back to Transactions"
        buttonLink="/transactions"
        startIcon={<ArrowBackIcon />}
      />

      <form onSubmit={handleSubmit(onSubmit)}>
        <FormSection title="Transaction Information">
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Controller
                name="valueDate"
                control={control}
                rules={{ required: 'Value Date is mandatory' }}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Value Date"
                    type="date"
                    fullWidth
                    required
                    InputLabelProps={{ shrink: true }}
                    error={!!errors.valueDate}
                    helperText={errors.valueDate?.message}
                    disabled={isLoading}
                  />
                )}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <Controller
                name="narration"
                control={control}
                render={({ field }) => (
                  <TextField
                    {...field}
                    label="Narration"
                    fullWidth
                    multiline
                    rows={1}
                    error={!!errors.narration}
                    helperText={errors.narration?.message}
                    disabled={true}
                  />
                )}
              />
            </Grid>
          </Grid>
        </FormSection>

        <Paper sx={{ p: 3, mb: 3 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h6">Transaction Lines</Typography>
          </Box>

          {fields.map((field, index) => (
            <Box key={field.id} mb={3} p={2} border="1px solid #e0e0e0" borderRadius={1}>
              <Grid container spacing={2}>
                <Grid item xs={12}>
                  <Box display="flex" justifyContent="space-between">
                    <Typography variant="subtitle1">Line {index + 1}</Typography>
                    {fields.length > 2 && (
                      <IconButton 
                        color="error" 
                        onClick={() => remove(index)}
                        disabled={isLoading}
                      >
                        <DeleteIcon />
                      </IconButton>
                    )}
                  </Box>
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name={`lines.${index}.accountNo`}
                    control={control}
                    rules={{ required: 'Account Number is required' }}
                    render={({ field }) => (
                      <Autocomplete
                        options={allAccounts}
                        getOptionLabel={(option) => option.displayName}
                        value={allAccounts.find(account => account.accountNo === field.value) || null}
                        onChange={(_, newValue) => {
                          const accountNo = newValue?.accountNo || '';
                          field.onChange(accountNo);
                          // Fetch balance when account is selected
                          fetchAccountBalance(accountNo, index);
                        }}
                        disabled={isLoading}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label="Account"
                            error={!!errors.lines?.[index]?.accountNo}
                            helperText={errors.lines?.[index]?.accountNo?.message}
                            placeholder="Search by account number or name..."
                          />
                        )}
                        renderOption={(props, option) => (
                          <Box component="li" {...props}>
                            <Box>
                              <Typography variant="body1" fontWeight="medium">
                                {option.accountNo} - {option.acctName}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                {option.accountType} Account
                              </Typography>
                            </Box>
                          </Box>
                        )}
                        isOptionEqualToValue={(option, value) => option.accountNo === value?.accountNo}
                        noOptionsText="No accounts found"
                        loading={isLoadingAccounts}
                        loadingText="Loading accounts..."
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <TextField
                    label="Available Balance"
                    type="text"
                    fullWidth
                    value={accountBalances.get(`${index}`)?.computedBalance?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'}
                    InputProps={{
                      readOnly: true,
                      startAdornment: (
                        <InputAdornment position="start">
                          BDT
                        </InputAdornment>
                      ),
                      endAdornment: loadingBalances.has(index) ? (
                        <InputAdornment position="end">
                          <CircularProgress size={20} />
                        </InputAdornment>
                      ) : null,
                    }}
                    disabled={true}
                    helperText={
                      accountOverdraftStatus.get(`${index}`) 
                        ? "💳 Overdraft account - negative balance allowed"
                        : `Previous Day Opening: ${accountBalances.get(`${index}`)?.availableBalance?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'} BDT`
                    }
                    sx={{
                      '& .MuiInputBase-root': {
                        backgroundColor: accountOverdraftStatus.get(`${index}`) ? '#fff3e0' : '#f5f5f5',
                        fontWeight: 'bold',
                        borderColor: accountOverdraftStatus.get(`${index}`) ? 'orange' : 'inherit'
                      }
                    }}
                  />
                </Grid>

                {/* Current Day Transaction Summary */}
                {accountBalances.get(`${index}`) && (
                  <Grid item xs={12}>
                    <Box sx={{ 
                      p: 2, 
                      backgroundColor: '#f8f9fa', 
                      borderRadius: 1, 
                      border: '1px solid #e9ecef',
                      fontSize: '0.875rem'
                    }}>
                      <Typography variant="subtitle2" sx={{ fontWeight: 'bold', mb: 1 }}>
                        Current Day Transaction Summary:
                      </Typography>
                      <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Typography variant="body2" color="text.secondary">
                          Previous Day Opening Balance:
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 'medium' }}>
                          {accountBalances.get(`${index}`)?.availableBalance?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'} BDT
                        </Typography>
                      </Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Typography variant="body2" color="text.secondary">
                          Today's Credits:
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 'medium', color: 'green' }}>
                          +{accountBalances.get(`${index}`)?.todayCredits?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'} BDT
                        </Typography>
                      </Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center">
                        <Typography variant="body2" color="text.secondary">
                          Today's Debits:
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 'medium', color: 'red' }}>
                          -{accountBalances.get(`${index}`)?.todayDebits?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'} BDT
                        </Typography>
                      </Box>
                      <Box display="flex" justifyContent="space-between" alignItems="center" sx={{ borderTop: '1px solid #dee2e6', pt: 1, mt: 1 }}>
                        <Typography variant="body2" sx={{ fontWeight: 'bold' }}>
                          Available Balance:
                        </Typography>
                        <Typography variant="body2" sx={{ fontWeight: 'bold', color: 'primary.main' }}>
                          {accountBalances.get(`${index}`)?.computedBalance?.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) || '0.00'} BDT
                        </Typography>
                      </Box>
                    </Box>
                  </Grid>
                )}

                <Grid item xs={12} md={6}>
                  <Controller
                    name={`lines.${index}.drCrFlag`}
                    control={control}
                    rules={{ required: 'Debit/Credit Flag is required' }}
                    render={({ field }) => (
                      <FormControl fullWidth error={!!errors.lines?.[index]?.drCrFlag} disabled={isLoading}>
                        <InputLabel id={`drcr-label-${index}`}>Dr/Cr</InputLabel>
                        <Select
                          {...field}
                          labelId={`drcr-label-${index}`}
                          label="Dr/Cr"
                        >
                          <MenuItem value={DrCrFlag.D}>Debit</MenuItem>
                          <MenuItem value={DrCrFlag.C}>Credit</MenuItem>
                        </Select>
                        <FormHelperText>{errors.lines?.[index]?.drCrFlag?.message}</FormHelperText>
                      </FormControl>
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={4}>
                  <Controller
                    name={`lines.${index}.tranCcy`}
                    control={control}
                    rules={{ required: 'Currency is required' }}
                    render={({ field }) => (
                      <FormControl fullWidth error={!!errors.lines?.[index]?.tranCcy} disabled={isLoading}>
                        <InputLabel id={`currency-label-${index}`}>Currency</InputLabel>
                        <Select
                          {...field}
                          labelId={`currency-label-${index}`}
                          label="Currency"
                        >
                          {CURRENCIES.map(currency => (
                            <MenuItem key={currency} value={currency}>{currency}</MenuItem>
                          ))}
                        </Select>
                        <FormHelperText>{errors.lines?.[index]?.tranCcy?.message}</FormHelperText>
                      </FormControl>
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={4}>
                  <Controller
                    name={`lines.${index}.fcyAmt`}
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Amount FCY"
                        type="number"
                        fullWidth
                        InputProps={{
                          readOnly: true,
                          startAdornment: (
                            <InputAdornment position="start">
                              {watch(`lines.${index}.tranCcy`)}
                            </InputAdornment>
                          ),
                        }}
                        error={!!errors.lines?.[index]?.fcyAmt}
                        helperText={errors.lines?.[index]?.fcyAmt?.message}
                        disabled={true}
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={4}>
                  <Controller
                    name={`lines.${index}.exchangeRate`}
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Exchange Rate"
                        type="number"
                        fullWidth
                        InputProps={{
                          readOnly: true,
                        }}
                        error={!!errors.lines?.[index]?.exchangeRate}
                        helperText={errors.lines?.[index]?.exchangeRate?.message}
                        disabled={true}
                      />
                    )}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name={`lines.${index}.lcyAmt`}
                    control={control}
                    rules={{ 
                      required: 'LCY Amount is required',
                      min: { value: 0.01, message: 'Amount must be greater than zero' }
                    }}
                    render={({ field }) => {
                      const currentLine = watchedLines?.[index];
                      const balance = accountBalances.get(`${index}`);
                      const isOverdraftAccount = accountOverdraftStatus.get(`${index}`) || false;
                      const isAssetAccount = assetAccounts.get(`${index}`) || false;
                      const isDebit = currentLine?.drCrFlag === DrCrFlag.D;
                      const exceedsBalance = isDebit && !isOverdraftAccount && !isAssetAccount && balance && field.value > balance.computedBalance;
                      
                      return (
                        <TextField
                          {...field}
                          label="Amount LCY"
                          type="number"
                          fullWidth
                          required
                          InputProps={{
                            startAdornment: (
                              <InputAdornment position="start">
                                BDT
                              </InputAdornment>
                            ),
                          }}
                          onChange={(e) => {
                            const inputValue = e.target.value;
                            // Allow empty string for clearing the field
                            if (inputValue === '' || inputValue === null || inputValue === undefined) {
                              field.onChange(0);
                              setValue(`lines.${index}.fcyAmt`, 0);
                              return;
                            }
                            const value = parseFloat(inputValue);
                            const finalValue = isNaN(value) ? 0 : value;
                            
                            // Update both LCY and FCY amounts
                            field.onChange(finalValue);
                            setValue(`lines.${index}.fcyAmt`, finalValue);
                            
                            // Force form re-render to update totals
                            console.log(`Amount updated for line ${index}: ${finalValue}`);
                          }}
                          error={!!errors.lines?.[index]?.lcyAmt || exceedsBalance}
                          helperText={
                            exceedsBalance 
                              ? `⚠️ Insufficient balance! Available: ${balance.computedBalance.toFixed(2)} BDT`
                              : isAssetAccount && isDebit
                              ? `💼 Asset Account - Available balance includes loan limit`
                              : isOverdraftAccount && isDebit
                              ? `💳 Overdraft account - negative balance allowed`
                              : errors.lines?.[index]?.lcyAmt?.message
                          }
                          disabled={isLoading}
                        />
                      );
                    }}
                  />
                </Grid>

                <Grid item xs={12} md={6}>
                  <Controller
                    name={`lines.${index}.udf1`}
                    control={control}
                    render={({ field }) => (
                      <TextField
                        {...field}
                        label="Narration"
                        fullWidth
                        error={!!errors.lines?.[index]?.udf1}
                        helperText={errors.lines?.[index]?.udf1?.message}
                        disabled={isLoading}
                      />
                    )}
                  />
                </Grid>
              </Grid>
            </Box>
          ))}

          {/* Add Line Button - positioned after last row */}
          <Box display="flex" justifyContent="center" mt={2}>
            <Button
              variant="outlined"
              startIcon={<AddIcon />}
              onClick={addLine}
              disabled={isLoading}
              sx={{ minWidth: 120 }}
            >
              Add Line
            </Button>
          </Box>

          {/* Transaction Totals - Updates Instantly as you type */}
          <Paper 
            variant="outlined" 
            sx={{ 
              p: 3, 
              mt: 2, 
              backgroundColor: isBalanced ? '#e8f5e9' : '#fff3e0',
              border: 2,
              borderColor: isBalanced ? 'success.main' : 'warning.main',
              transition: 'all 0.3s ease'
            }}
          >
            <Typography variant="subtitle2" sx={{ mb: 2, color: 'text.secondary' }}>
              Transaction Summary (Updates Instantly)
            </Typography>
            <Grid container spacing={2}>
              <Grid item xs={12} md={4}>
                <Typography variant="subtitle1" fontWeight="bold" color="text.secondary">
                  Total Debit:
                </Typography>
                <Typography 
                  variant="h6" 
                  color="primary.main"
                  sx={{ 
                    fontWeight: 'bold',
                    fontSize: '1.5rem',
                    transition: 'color 0.3s ease'
                  }}
                >
                  {totalDebit.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} BDT
                </Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="subtitle1" fontWeight="bold" color="text.secondary">
                  Total Credit:
                </Typography>
                <Typography 
                  variant="h6" 
                  color="secondary.main"
                  sx={{ 
                    fontWeight: 'bold',
                    fontSize: '1.5rem',
                    transition: 'color 0.3s ease'
                  }}
                >
                  {totalCredit.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} BDT
                </Typography>
              </Grid>
              <Grid item xs={12} md={4}>
                <Typography variant="subtitle1" fontWeight="bold" color="text.secondary">
                  Difference:
                </Typography>
                <Typography 
                  variant="h6" 
                  color={isBalanced ? 'success.main' : 'error.main'}
                  sx={{ 
                    fontWeight: 'bold',
                    fontSize: '1.5rem',
                    transition: 'color 0.3s ease'
                  }}
                >
                  {Math.abs(totalDebit - totalCredit).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} BDT
                  {isBalanced && ' ✓'}
                </Typography>
              </Grid>
            </Grid>
          </Paper>

          {!isBalanced && (
            <Alert severity="warning" sx={{ mt: 2 }}>
              Transaction is not balanced. Total debit must equal total credit.
            </Alert>
          )}
        </Paper>

        <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end', gap: 2 }}>
          <Button
            component={RouterLink}
            to="/transactions"
            variant="outlined"
            disabled={isLoading}
          >
            Cancel
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={isLoading || !isBalanced || fields.length < 2}
            startIcon={isLoading ? <CircularProgress size={20} /> : <SaveIcon />}
          >
            Create Transaction (Entry)
          </Button>
        </Box>
      </form>
    </Box>
  );
};

export default TransactionForm;
