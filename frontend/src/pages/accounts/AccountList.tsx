import { Add as AddIcon, Visibility as ViewIcon, Close as CloseIcon, Edit as EditIcon, Search as SearchIcon } from '@mui/icons-material';
import { Box, IconButton, Tooltip, Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button, TextField, InputAdornment } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useMemo, useEffect } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { toast } from 'react-toastify';
import { closeCustomerAccount, getAllCustomerAccounts } from '../../api/customerAccountService';
import { DataTable, PageHeader, StatusBadge, ErrorDisplay } from '../../components/common';
import type { Column } from '../../components/common';
import { AccountStatus } from '../../types';
import type { CustomerAccountResponseDTO } from '../../types';

const AccountList = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [, setSort] = useState<string | undefined>(undefined);
  const [searchTerm, setSearchTerm] = useState('');
  const [closeConfirmDialog, setCloseConfirmDialog] = useState<{
    open: boolean;
    account: CustomerAccountResponseDTO | null;
  }>({
    open: false,
    account: null,
  });

  const queryClient = useQueryClient();

  // Fetch all accounts at once for client-side filtering
  const { data: allAccounts, isLoading, error, refetch } = useQuery({
    queryKey: ['accounts', 'all'],
    queryFn: () => getAllCustomerAccounts(0, 1000), // Get a large number to effectively get all
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
        (account.custName && account.custName.toLowerCase().includes(lowerCaseSearch)) ||
        (account.subProductName && account.subProductName.toLowerCase().includes(lowerCaseSearch)) ||
        String(account.currentBalance).includes(lowerCaseSearch) ||
        String(account.availableBalance).includes(lowerCaseSearch) ||
        account.accountStatus.toLowerCase().includes(lowerCaseSearch)
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
    console.error('Error fetching customer accounts:', error);
  }

  // Close account mutation
  const closeAccountMutation = useMutation({
    mutationFn: closeCustomerAccount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['accounts'] });
      toast.success('Account closed successfully');
      handleCloseDialog();
    },
    onError: (error: Error) => {
      toast.error(`Failed to close account: ${error.message}`);
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
  const handleOpenCloseDialog = (account: CustomerAccountResponseDTO) => {
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
    if (!closeConfirmDialog.account) return;
    
    closeAccountMutation.mutate(closeConfirmDialog.account.accountNo);
  };

  // Table columns
  const columns: Column<CustomerAccountResponseDTO>[] = [
    { id: 'accountNo', label: 'Account Number', minWidth: 150, sortable: true },
    { id: 'acctName', label: 'Account Name', minWidth: 180, sortable: true },
    { 
      id: 'custName', 
      label: 'Customer Name', 
      minWidth: 180,
      format: (value: string | null | undefined) => value || 'N/A'
    },
    { id: 'subProductName', label: 'Product', minWidth: 150 },
    { 
      id: 'currentBalance', 
      label: 'Current Balance', 
      minWidth: 120,
      align: 'right',
      format: (value: number | null | undefined) => (value !== null && value !== undefined) ? `${value.toLocaleString()}` : 'N/A'
    },
    { 
      id: 'availableBalance', 
      label: 'Available Balance', 
      minWidth: 120,
      align: 'right',
      format: (value: number | null | undefined) => (value !== null && value !== undefined) ? `${value.toLocaleString()}` : 'N/A'
    },
    { 
      id: 'accountStatus', 
      label: 'Status', 
      minWidth: 100,
      format: (value: AccountStatus | null | undefined) => (
        <StatusBadge status={value || 'UNKNOWN'} />
      )
    },
    { 
      id: 'actions', 
      label: 'Actions', 
      minWidth: 100,
      format: (_, row: CustomerAccountResponseDTO) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
          <Tooltip title="Edit">
            <IconButton 
              component={RouterLink} 
              to={`/accounts/edit/${row.accountNo}`} 
              color="primary"
              size="small"
              sx={{ padding: '4px' }}
            >
              <EditIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip title="View Details">
            <IconButton 
              component={RouterLink} 
              to={`/accounts/${row.accountNo}`} 
              color="primary"
              size="small"
              sx={{ padding: '4px' }}
            >
              <ViewIcon fontSize="small" />
            </IconButton>
          </Tooltip>
          {row.accountStatus === AccountStatus.ACTIVE && (row.currentBalance === 0 || row.currentBalance === null) && (
            <Tooltip title="Close Account">
              <IconButton 
                color="error" 
                onClick={() => handleOpenCloseDialog(row)}
                size="small"
                sx={{ padding: '4px' }}
              >
                <CloseIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      )
    },
  ];

  return (
    <Box>
      <PageHeader
        title="Customer Account Management"
        buttonText="Create Account"
        buttonLink="/accounts/new"
        startIcon={<AddIcon />}
      />

      {/* Search Panel - Right aligned */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 3 }}>
        <TextField
          value={searchTerm}
          onChange={handleSearchInputChange}
          placeholder="Search accounts..."
          variant="outlined"
          size="small"
          sx={{ width: '300px' }}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon color="action" />
              </InputAdornment>
            ),
            endAdornment: searchTerm && (
              <InputAdornment position="end">
                <IconButton 
                  aria-label="clear search"
                  onClick={() => setSearchTerm('')}
                  edge="end"
                  size="small"
                >
                  <Tooltip title="Clear search">
                    <Box component="span" sx={{ display: 'flex' }}>×</Box>
                  </Tooltip>
                </IconButton>
              </InputAdornment>
            )
          }}
        />
      </Box>

      {error ? (
        <ErrorDisplay 
          error={error} 
          title="Error Loading Accounts" 
          onRetry={refetch}
        />
      ) : (
        <DataTable
          columns={columns}
          rows={paginatedData}
          totalItems={filteredAccounts.length}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={setRowsPerPage}
          onSort={handleSort}
          loading={isLoading}
          idField="accountNo"
        />
      )}

      {/* Close Account Confirmation Dialog */}
      <Dialog
        open={closeConfirmDialog.open}
        onClose={handleCloseDialog}
        aria-labelledby="alert-dialog-title"
        aria-describedby="alert-dialog-description"
      >
        <DialogTitle id="alert-dialog-title">
          Close Account Confirmation
        </DialogTitle>
        <DialogContent>
          <DialogContentText id="alert-dialog-description">
            Are you sure you want to close account {closeConfirmDialog.account?.accountNo}?
            This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseDialog}>Cancel</Button>
          <Button 
            onClick={handleCloseAccount} 
            color="error" 
            autoFocus
            disabled={closeAccountMutation.isPending}
          >
            {closeAccountMutation.isPending ? 'Closing...' : 'Close Account'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default AccountList;
