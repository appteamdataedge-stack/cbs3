/**
 * Statement of Accounts (SOA) Module
 * Allows users to generate account statements for any account with a maximum 6-month date range
 */

import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  CircularProgress,
  FormControl,
  FormControlLabel,
  FormLabel,
  Grid,
  Paper,
  Radio,
  RadioGroup,
  TextField,
  Typography,
  Alert,
} from '@mui/material';
import { Description as DescriptionIcon } from '@mui/icons-material';
import type { AccountOption } from '../types/soa.types';
import { getAccountList, generateSOA } from '../services/soaService';
import { toast } from 'react-toastify';
import { PageHeader, FormSection } from '../components/common';
import Select from 'react-select';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';

const StatementOfAccounts: React.FC = () => {
  // State variables
  const [selectedAccount, setSelectedAccount] = useState<string>('');
  const [fromDate, setFromDate] = useState<Date | null>(null);
  const [toDate, setToDate] = useState<Date | null>(null);
  const [format, setFormat] = useState<string>('excel');
  const [accounts, setAccounts] = useState<AccountOption[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [dateRangeError, setDateRangeError] = useState<string | null>(null);

  // Fetch account list on component mount
  useEffect(() => {
    const fetchAccounts = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const accountList = await getAccountList();
        setAccounts(accountList);
      } catch (err: any) {
        setError(err.message || 'Failed to load account list');
        toast.error('Failed to load account list');
      } finally {
        setIsLoading(false);
      }
    };

    fetchAccounts();
  }, []);

  // Handler: Account selection change
  const handleAccountChange = (selectedOption: any) => {
    setSelectedAccount(selectedOption ? selectedOption.value : '');
    setError(null);
  };

  // Handler: From date change
  const handleFromDateChange = (date: Date | null) => {
    setFromDate(date);
    if (date && toDate) {
      validateDateRange(date, toDate);
    } else {
      setDateRangeError(null);
    }
  };

  // Handler: To date change
  const handleToDateChange = (date: Date | null) => {
    setToDate(date);
    if (fromDate && date) {
      validateDateRange(fromDate, date);
    } else {
      setDateRangeError(null);
    }
  };

  // Validate date range (6-month maximum)
  const validateDateRange = (from: Date, to: Date) => {
    // Check if from date is after to date
    if (from > to) {
      setDateRangeError('From date must be before or equal to To date');
      return false;
    }

    // Calculate months between dates
    const monthsDiff = (to.getFullYear() - from.getFullYear()) * 12 + 
                       (to.getMonth() - from.getMonth());

    if (monthsDiff > 6) {
      setDateRangeError('Maximum 6 months date range allowed');
      return false;
    }

    setDateRangeError(null);
    return true;
  };

  // Handler: Form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // Validate all fields
    if (!selectedAccount) {
      toast.error('Please select an account');
      return;
    }

    if (!fromDate) {
      toast.error('Please select from date');
      return;
    }

    if (!toDate) {
      toast.error('Please select to date');
      return;
    }

    if (dateRangeError) {
      toast.error(dateRangeError);
      return;
    }

    // Generate SOA
    setIsGenerating(true);
    try {
      await generateSOA(selectedAccount, fromDate, toDate, format);
      toast.success('Statement generated successfully');
    } catch (err: any) {
      toast.error(err.message || 'Failed to generate statement');
    } finally {
      setIsGenerating(false);
    }
  };

  // Handler: Clear form
  const handleClear = () => {
    setSelectedAccount('');
    setFromDate(null);
    setToDate(null);
    setFormat('excel');
    setError(null);
    setDateRangeError(null);
  };

  // Prepare options for react-select
  const accountOptions = accounts.map(account => ({
    value: account.accountNo,
    label: `${account.accountNo} - ${account.accountName} (${account.accountType})`
  }));

  // Check if form is valid
  const isFormValid = selectedAccount && fromDate && toDate && !dateRangeError && !isGenerating;

  return (
    <Box>
      <PageHeader
        title="Statement of Accounts"
        subtitle="Generate account statements with a maximum 6-month date range"
        startIcon={<DescriptionIcon />}
        compact
      />

      {/* Error Display */}
      {error && (
        <Alert severity="error" sx={{ mb: 1.5 }}>
          {error}
        </Alert>
      )}

      <form onSubmit={handleSubmit}>
        <FormSection title="Statement Information" compact>
          <Grid container spacing={2}>
            {/* Account Number Field */}
            <Grid item xs={12}>
              <FormControl fullWidth>
                <Typography variant="body2" gutterBottom sx={{ fontWeight: 'medium', mb: 0.5, fontSize: '0.875rem' }}>
                  Account Number <span style={{ color: 'error.main' }}>*</span>
                </Typography>
                {isLoading ? (
                  <Box display="flex" justifyContent="center" py={1.5}>
                    <CircularProgress size={20} />
                  </Box>
                ) : (
                  <Select
                    options={accountOptions}
                    onChange={handleAccountChange}
                    placeholder="Select account..."
                    isSearchable={true}
                    isClearable={true}
                    styles={{
                      control: (base) => ({
                        ...base,
                        minHeight: '42px',
                        borderRadius: '4px',
                        borderColor: '#c4c4c4',
                        fontSize: '0.875rem',
                        '&:hover': {
                          borderColor: '#1976d2',
                        },
                      }),
                      menu: (base) => ({
                        ...base,
                        zIndex: 9999,
                      }),
                      option: (base) => ({
                        ...base,
                        fontSize: '0.875rem',
                      }),
                    }}
                  />
                )}
              </FormControl>
            </Grid>

            {/* Date Range Fields */}
            <Grid item xs={12} md={6}>
              <Typography variant="body2" gutterBottom sx={{ fontWeight: 'medium', mb: 0.5, fontSize: '0.875rem' }}>
                From Date <span style={{ color: 'error.main' }}>*</span>
              </Typography>
              <DatePicker
                selected={fromDate}
                onChange={handleFromDateChange}
                maxDate={new Date()}
                dateFormat="dd-MMM-yyyy"
                placeholderText="Select from date"
                customInput={
                  <TextField
                    fullWidth
                    required
                    size="small"
                    InputProps={{
                      style: { cursor: 'pointer', fontSize: '0.875rem' },
                    }}
                  />
                }
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Typography variant="body2" gutterBottom sx={{ fontWeight: 'medium', mb: 0.5, fontSize: '0.875rem' }}>
                To Date <span style={{ color: 'error.main' }}>*</span>
              </Typography>
              <DatePicker
                selected={toDate}
                onChange={handleToDateChange}
                minDate={fromDate || undefined}
                maxDate={new Date()}
                dateFormat="dd-MMM-yyyy"
                placeholderText="Select to date"
                customInput={
                  <TextField
                    fullWidth
                    required
                    size="small"
                    InputProps={{
                      style: { cursor: 'pointer', fontSize: '0.875rem' },
                    }}
                  />
                }
              />
            </Grid>

            {/* Date Range Error */}
            {dateRangeError && (
              <Grid item xs={12}>
                <Alert severity="error">{dateRangeError}</Alert>
              </Grid>
            )}

            {/* Format Field */}
            <Grid item xs={12}>
              <FormControl component="fieldset">
                <FormLabel component="legend" sx={{ fontWeight: 'medium', mb: 0.5, fontSize: '0.875rem' }}>
                  Format
                </FormLabel>
                <RadioGroup
                  row
                  value={format}
                  onChange={(e) => setFormat(e.target.value)}
                >
                  <FormControlLabel
                    value="excel"
                    control={<Radio size="small" />}
                    label={<Typography variant="body2" fontSize="0.875rem">Excel</Typography>}
                  />
                  <FormControlLabel
                    value="pdf"
                    control={<Radio size="small" />}
                    label={<Typography variant="body2" fontSize="0.875rem">PDF (Coming Soon)</Typography>}
                    disabled
                  />
                </RadioGroup>
              </FormControl>
            </Grid>

            {/* Action Buttons */}
            <Grid item xs={12}>
              <Box display="flex" gap={1.5} flexDirection={{ xs: 'column', sm: 'row' }}>
                <Button
                  type="submit"
                  variant="contained"
                  color="primary"
                  disabled={!isFormValid}
                  fullWidth
                  size="medium"
                  sx={{ py: 0.75, fontSize: '0.875rem' }}
                  startIcon={isGenerating ? <CircularProgress size={16} color="inherit" /> : <DescriptionIcon fontSize="small" />}
                >
                  {isGenerating ? 'Generating...' : 'Generate Statement'}
                </Button>

                <Button
                  type="button"
                  variant="outlined"
                  color="secondary"
                  onClick={handleClear}
                  fullWidth
                  size="medium"
                  sx={{ py: 0.75, fontSize: '0.875rem' }}
                >
                  Clear
                </Button>
              </Box>
            </Grid>
          </Grid>
        </FormSection>

        {/* Information Section */}
        <Paper sx={{ p: 2, bgcolor: 'info.lighter', border: 1, borderColor: 'info.light' }}>
          <Typography variant="subtitle2" gutterBottom sx={{ color: 'info.dark', fontWeight: 'bold', fontSize: '0.9375rem', mb: 0.75 }}>
            Information
          </Typography>
          <Box component="ul" sx={{ pl: 2.5, color: 'info.dark', m: 0 }}>
            <Typography component="li" variant="body2" sx={{ mb: 0.5, fontSize: '0.8125rem' }}>
              Maximum date range: 6 months
            </Typography>
            <Typography component="li" variant="body2" sx={{ mb: 0.5, fontSize: '0.8125rem' }}>
              Statements include all verified transactions
            </Typography>
            <Typography component="li" variant="body2" sx={{ mb: 0.5, fontSize: '0.8125rem' }}>
              Opening and closing balances are calculated automatically
            </Typography>
            <Typography component="li" variant="body2" sx={{ fontSize: '0.8125rem' }}>
              Excel format includes transaction details, debits, credits, and running balance
            </Typography>
          </Box>
        </Paper>
      </form>
    </Box>
  );
};

export default StatementOfAccounts;

