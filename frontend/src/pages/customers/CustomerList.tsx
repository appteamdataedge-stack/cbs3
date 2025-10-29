import { Add as AddIcon, Edit as EditIcon, Verified as VerifiedIcon, Search as SearchIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { Box, IconButton, Tooltip, TextField, InputAdornment } from '@mui/material';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, useMemo, useEffect } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAllCustomers, verifyCustomer } from '../../api/customerService';
import { DataTable, PageHeader, StatusBadge, VerificationModal, ErrorDisplay } from '../../components/common';
import type { Column } from '../../components/common';
import { CustomerType } from '../../types';
import type { CustomerResponseDTO, CustomerVerificationDTO } from '../../types';

const CustomerList = () => {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [, setSort] = useState<string | undefined>(undefined);
  const [searchTerm, setSearchTerm] = useState('');
  const [verificationModal, setVerificationModal] = useState<{
    open: boolean;
    customerId: number | null;
  }>({
    open: false,
    customerId: null,
  });

  // Fetch all customers at once
  const { data: allCustomers, isLoading, error, refetch } = useQuery({
    queryKey: ['customers', 'all'],
    queryFn: () => getAllCustomers(0, 1000), // Get a large number to effectively get all
  });
  
  // Filter customers based on search term
  const filteredCustomers = useMemo(() => {
    if (!allCustomers?.content || searchTerm.trim() === '') {
      return allCustomers?.content || [];
    }
    
    const lowerCaseSearch = searchTerm.toLowerCase();
    
    return allCustomers.content.filter((customer) => {
      // Get the customer name based on type
      const customerName = customer.custType === CustomerType.INDIVIDUAL
        ? `${customer.firstName || ''} ${customer.lastName || ''}`.trim()
        : customer.tradeName || '';
      
      // Search in various fields
      return (
        customerName.toLowerCase().includes(lowerCaseSearch) || 
        customer.extCustId.toLowerCase().includes(lowerCaseSearch) ||
        String(customer.custId).includes(lowerCaseSearch) ||
        (customer.mobile && customer.mobile.toLowerCase().includes(lowerCaseSearch))
      );
    });
  }, [allCustomers, searchTerm]);
  
  // Paginated data for the table
  const paginatedData = useMemo(() => {
    const startIndex = page * rowsPerPage;
    const endIndex = startIndex + rowsPerPage;
    return filteredCustomers.slice(startIndex, endIndex);
  }, [filteredCustomers, page, rowsPerPage]);
  
  // Reset to first page when search changes
  useEffect(() => {
    setPage(0);
  }, [searchTerm]);
  
  // Handle search input change - dynamic search as user types
  const handleSearchInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  };

  // Handle sort
  const handleSort = (field: string, direction: 'asc' | 'desc') => {
    setSort(`${field},${direction}`);
  };

  // Handle verify
  const handleOpenVerifyModal = (customerId: number) => {
    setVerificationModal({
      open: true,
      customerId,
    });
  };

  const handleCloseVerifyModal = () => {
    setVerificationModal({
      open: false,
      customerId: null,
    });
  };

  const handleVerify = async (verifierId: string) => {
    if (!verificationModal.customerId) return;

    try {
      const verificationData: CustomerVerificationDTO = { verifierId };
      await verifyCustomer(verificationModal.customerId, verificationData);
      toast.success('Customer verified successfully');
      // Invalidate customer queries to refresh all customer data
      queryClient.invalidateQueries({ queryKey: ['customers'] });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to verify customer');
      throw err; // Re-throw to let the modal component handle the error
    }
  };

  // Table columns
  const columns: Column<CustomerResponseDTO>[] = [
    { id: 'custId', label: 'ID', minWidth: 50, sortable: true },
    { id: 'extCustId', label: 'External ID', minWidth: 100, sortable: true },
    { 
      id: 'custType', 
      label: 'Type', 
      minWidth: 100,
      format: (value: CustomerType) => value
    },
    { 
      id: 'customerName', 
      label: 'Name', 
      minWidth: 170,
      format: (_, row: CustomerResponseDTO) => {
        if (row.custType === CustomerType.INDIVIDUAL) {
          return `${row.firstName || ''} ${row.lastName || ''}`.trim();
        } else {
          return row.tradeName || '';
        }
      }
    },
    { id: 'mobile', label: 'Mobile', minWidth: 120 },
    { 
      id: 'verified', 
      label: 'Status', 
      minWidth: 100,
      format: (value: boolean) => (
        <StatusBadge status={value ? 'VERIFIED' : 'PENDING'} />
      )
    },
    { 
      id: 'actions', 
      label: 'Actions', 
      minWidth: 120,
      format: (_, row: CustomerResponseDTO) => (
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="View Details">
            <IconButton 
              component={RouterLink} 
              to={`/customers/view/${row.custId}`} 
              color="info"
              size="small"
            >
              <ViewIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Edit">
            <IconButton 
              component={RouterLink} 
              to={`/customers/edit/${row.custId}`} 
              color="primary"
              size="small"
            >
              <EditIcon />
            </IconButton>
          </Tooltip>
          {!row.verified && (
            <Tooltip title="Verify">
              <IconButton 
                color="success" 
                onClick={() => handleOpenVerifyModal(row.custId)}
                size="small"
              >
                <VerifiedIcon />
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
        title="Customer Management"
        buttonText="Add Customer"
        buttonLink="/customers/new"
        startIcon={<AddIcon />}
      />

      {/* Search Panel - Right aligned */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 3 }}>
        <TextField
          value={searchTerm}
          onChange={handleSearchInputChange}
          placeholder="Search customers..."
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
          title="Error Loading Customers" 
          onRetry={refetch}
        />
      ) : (
        <DataTable
          columns={columns}
          rows={paginatedData}
          totalItems={filteredCustomers.length}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={setRowsPerPage}
          onSort={handleSort}
          loading={isLoading}
          idField="custId"
        />
      )}

      <VerificationModal
        open={verificationModal.open}
        onClose={handleCloseVerifyModal}
        onVerify={handleVerify}
        title="Verify Customer"
        description="Please enter your user ID to verify this customer."
      />
    </Box>
  );
};

export default CustomerList;
