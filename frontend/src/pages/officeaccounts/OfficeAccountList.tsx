import { Add as AddIcon, Visibility as ViewIcon, Close as CloseIcon, Edit as EditIcon, Search as SearchIcon } from '@mui/icons-material';
import { Box, IconButton, Tooltip, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, TextField, InputAdornment } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useMemo, useEffect } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { toast } from 'react-toastify';
import { closeOfficeAccount, getAllOfficeAccounts } from '../../api/officeAccountService';
import { DataTable, PageHeader, StatusBadge, ErrorDisplay } from '../../components/common';
import type { Column } from '../../components/common';
import { AccountStatus } from '../../types';
import type { OfficeAccountResponseDTO } from '../../types';

const OfficeAccountList = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [, setSort] = useState<string | undefined>(undefined);
  const [searchTerm, setSearchTerm] = useState('');
  const [closeConfirmDialog, setCloseConfirmDialog] = useState<{
    open: boolean;
    account: OfficeAccountResponseDTO | null;
  }>({
    open: false,
    account: null,
  });

  const queryClient = useQueryClient();

  // Fetch all office accounts at once for client-side filtering
  const { data: allAccounts, isLoading, error, refetch } = useQuery({
    queryKey: ['officeAccounts', 'all'],
    queryFn: () => getAllOfficeAccounts(0, 1000), // Get a large number to effectively get all
    retry: 3,
    retryDelay: 1000
  });
  
  // Filter accounts based on search term
  const filteredAccounts = useMemo(() => {
    if (!allAccounts?.content || searchTerm.trim() === '') {
      return allAccounts?.content || [];
    }
    
    const lowerCaseSearch = searchTerm.toLowerCase();
    
    return allAccounts.content.filter((account) => {
      // Search in various fields
      return (
        account.accountNo.toLowerCase().includes(lowerCaseSearch) || 
        account.acctName.toLowerCase().includes(lowerCaseSearch) ||
        account.subProductName?.toLowerCase().includes(lowerCaseSearch) ||
        account.glNum?.toLowerCase().includes(lowerCaseSearch) ||
        account.branchCode.toLowerCase().includes(lowerCaseSearch) ||
        account.accountStatus.toLowerCase().includes(lowerCaseSearch) ||
        String(account.reconciliationRequired).toLowerCase().includes(lowerCaseSearch)
      );
    });
  }, [allAccounts, searchTerm]);
  
  // Paginated data for the table
  const paginatedData = useMemo(() => {
    const startIndex = page * rowsPerPage;
    const endIndex = startIndex + rowsPerPage;
    return filteredAccounts.slice(startIndex, endIndex);
  }, [filteredAccounts, page, rowsPerPage]);
  
  // Reset to first page when search changes
  useEffect(() => {
    setPage(0);
  }, [searchTerm]);
  
  // Handle error if needed
  if (error) {
    console.error('Error fetching office accounts:', error);
  }

  // Close account mutation
  const closeAccountMutation = useMutation({
    mutationFn: closeOfficeAccount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['officeAccounts'] });
      toast.success('Office account closed successfully');
      handleCloseDialog();
    },
    onError: (error: Error) => {
      toast.error(`Failed to close office account: ${error.message}`);
      handleCloseDialog();
    },
  });

  // Handle search input change - dynamic search as user types
  const handleSearchInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  };

  // Handle sort
  const handleSort = (field: string, direction: 'asc' | 'desc') => {
    setSort(`${field},${direction}`);
  };

  // Handle close account
  const handleOpenCloseDialog = (account: OfficeAccountResponseDTO) => {
    setCloseConfirmDialog({
      open: true,
      account,
    });
  };

  const handleCloseDialog = () => {
    setCloseConfirmDialog({
      open: false,
      account: null,
    });
  };

  const handleCloseAccount = () => {
    if (closeConfirmDialog.account) {
      closeAccountMutation.mutate(closeConfirmDialog.account.accountNo);
    }
  };

  // Define columns for the DataTable
  const columns: Column<OfficeAccountResponseDTO>[] = [
    {
      id: 'accountNo',
      label: 'Account Number',
      minWidth: 140,
      format: (value: string) => value,
    },
    {
      id: 'acctName',
      label: 'Account Name',
      minWidth: 200,
      format: (value: string) => value,
    },
    {
      id: 'subProductName',
      label: 'Sub Product',
      minWidth: 150,
      format: (value: string) => value || 'N/A',
    },
    {
      id: 'glNum',
      label: 'GL Number',
      minWidth: 120,
      format: (value: string) => value || 'N/A',
    },
    {
      id: 'branchCode',
      label: 'Branch Code',
      minWidth: 100,
      format: (value: string) => value,
    },
    {
      id: 'dateOpening',
      label: 'Opening Date',
      minWidth: 120,
      format: (value: string) => new Date(value).toLocaleDateString(),
    },
    {
      id: 'accountStatus',
      label: 'Status',
      minWidth: 100,
      format: (value: AccountStatus) => <StatusBadge status={value} />,
    },
    {
      id: 'reconciliationRequired',
      label: 'Reconciliation',
      minWidth: 120,
      format: (value: boolean) => (
        <StatusBadge 
          status={value ? 'Required' : 'Not Required'} 
          statusMap={{
            'Required': 'success',
            'Not Required': 'default'
          }}
        />
      ),
    },
    {
      id: 'actions',
      label: 'Actions',
      minWidth: 120,
      align: 'center',
      format: (_, account: OfficeAccountResponseDTO) => (
        <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center' }}>
          <Tooltip title="View Details">
            <IconButton
              component={RouterLink}
              to={`/office-accounts/${account.accountNo}`}
              size="small"
              color="primary"
            >
              <ViewIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Edit Account">
            <IconButton
              component={RouterLink}
              to={`/office-accounts/edit/${account.accountNo}`}
              size="small"
              color="secondary"
            >
              <EditIcon />
            </IconButton>
          </Tooltip>
          {account.accountStatus !== AccountStatus.CLOSED && (
            <Tooltip title="Close Account">
              <IconButton
                onClick={() => handleOpenCloseDialog(account)}
                size="small"
                color="error"
              >
                <CloseIcon />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      ),
    },
  ];

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <PageHeader 
          title="Office Accounts" 
          subtitle="Manage office accounts (non-customer accounts)"
          buttonText="New Office Account"
          buttonLink="/office-accounts/new"
          startIcon={<AddIcon />}
        />
        <ErrorDisplay 
          error={error} 
          onRetry={refetch}
          title="Failed to load office accounts"
        />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <PageHeader 
        title="Office Account Management" 
        buttonText="New Office Account"
        buttonLink="/office-accounts/new"
        startIcon={<AddIcon />}
      />

      {/* Search Bar */}
      <Box sx={{ mb: 3 }}>
        <TextField
          fullWidth
          variant="outlined"
          placeholder="Search office accounts by account number, name, sub product, GL number, branch code, or status..."
          value={searchTerm}
          onChange={handleSearchInputChange}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          sx={{ maxWidth: 600 }}
        />
      </Box>

      {/* Data Table */}
      <DataTable<OfficeAccountResponseDTO>
        columns={columns}
        rows={paginatedData}
        loading={isLoading}
        totalItems={filteredAccounts.length}
        page={page}
        rowsPerPage={rowsPerPage}
        onPageChange={setPage}
        onRowsPerPageChange={setRowsPerPage}
        onSort={handleSort}
      />

      {/* Close Account Confirmation Dialog */}
      <Dialog
        open={closeConfirmDialog.open}
        onClose={handleCloseDialog}
        aria-labelledby="close-account-dialog-title"
        aria-describedby="close-account-dialog-description"
      >
        <DialogTitle id="close-account-dialog-title">
          Close Office Account
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="close-account-dialog-description">
            Are you sure you want to close the office account{' '}
            <strong>{closeConfirmDialog.account?.accountNo}</strong> -{' '}
            <strong>{closeConfirmDialog.account?.acctName}</strong>?
            <br />
            <br />
            This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog} color="primary">
            Cancel
          </Button>
          <Button 
            onClick={handleCloseAccount} 
            color="error" 
            variant="contained"
            disabled={closeAccountMutation.isPending}
          >
            {closeAccountMutation.isPending ? 'Closing...' : 'Close Account'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default OfficeAccountList;
