import { Add as AddIcon, Edit as EditIcon, Verified as VerifiedIcon, Search as SearchIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { Box, IconButton, Tooltip, TextField, InputAdornment } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState, useMemo, useEffect } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAllSubProducts, verifySubProduct } from '../../api/subProductService';
import { DataTable, PageHeader, StatusBadge, VerificationModal, ErrorDisplay } from '../../components/common';
import type { Column } from '../../components/common';
import type { CustomerVerificationDTO, SubProductResponseDTO, SubProductStatus } from '../../types';

const SubProductList = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [, setSort] = useState<string | undefined>(undefined);
  const [searchTerm, setSearchTerm] = useState('');
  const [verificationModal, setVerificationModal] = useState<{
    open: boolean;
    subProductId: number | null;
  }>({
    open: false,
    subProductId: null,
  });

  // Fetch all subproducts at once
  const { data: allSubProducts, isLoading, error, refetch } = useQuery({
    queryKey: ['subproducts', 'all'],
    queryFn: () => getAllSubProducts(0, 1000), // Get a large number to effectively get all
    retry: 3,
    retryDelay: 1000
  });
  
  // Filter subproducts based on search term
  const filteredSubProducts = useMemo(() => {
    if (!allSubProducts?.content || searchTerm.trim() === '') {
      return allSubProducts?.content || [];
    }
    
    const lowerCaseSearch = searchTerm.toLowerCase();
    
    return allSubProducts.content.filter((subProduct) => {
      // Search in various fields
      return (
        subProduct.subProductName.toLowerCase().includes(lowerCaseSearch) || 
        subProduct.subProductCode.toLowerCase().includes(lowerCaseSearch) ||
        String(subProduct.subProductId).includes(lowerCaseSearch) ||
        String(subProduct.productId).includes(lowerCaseSearch) ||
        (subProduct.productName && subProduct.productName.toLowerCase().includes(lowerCaseSearch)) ||
        (subProduct.makerId && subProduct.makerId.toLowerCase().includes(lowerCaseSearch)) ||
        (subProduct.inttCode && subProduct.inttCode.toLowerCase().includes(lowerCaseSearch)) ||
        (subProduct.subProductStatus && subProduct.subProductStatus.toLowerCase().includes(lowerCaseSearch))
      );
    });
  }, [allSubProducts, searchTerm]);
  
  // Paginated data for the table
  const paginatedData = useMemo(() => {
    const startIndex = page * rowsPerPage;
    const endIndex = startIndex + rowsPerPage;
    return filteredSubProducts.slice(startIndex, endIndex);
  }, [filteredSubProducts, page, rowsPerPage]);
  
  // Reset to first page when search changes
  useEffect(() => {
    setPage(0);
  }, [searchTerm]);

  // Handle search input change - dynamic search as user types
  const handleSearchInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  };

  // Handle error if needed
  if (error) {
    console.error('Error fetching subproducts:', error);
  }

  // Handle sort
  const handleSort = (field: string, direction: 'asc' | 'desc') => {
    setSort(`${field},${direction}`);
  };

  // Handle verify
  const handleOpenVerifyModal = (subProductId: number) => {
    setVerificationModal({
      open: true,
      subProductId,
    });
  };

  const handleCloseVerifyModal = () => {
    setVerificationModal({
      open: false,
      subProductId: null,
    });
  };

  const handleVerify = async (verifierId: string) => {
    if (!verificationModal.subProductId) return;

    try {
      const verificationData: CustomerVerificationDTO = { verifierId };
      await verifySubProduct(verificationModal.subProductId, verificationData);
      toast.success('SubProduct verified successfully');
      refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to verify subproduct');
      throw err; // Re-throw to let the modal component handle the error
    }
  };

  // Table columns
  const columns: Column<SubProductResponseDTO>[] = [
    { id: 'subProductId', label: 'ID', minWidth: 50, sortable: true },
    { id: 'subProductCode', label: 'SubProduct Code', minWidth: 120, sortable: true },
    { id: 'subProductName', label: 'SubProduct Name', minWidth: 180, sortable: true },
    { 
      id: 'productName', 
      label: 'Product Name', 
      minWidth: 150,
      format: (value: string | undefined) => value || 'N/A'
    },
    { 
      id: 'interestRate', 
      label: 'Interest Rate', 
      minWidth: 100,
      format: (value: number | null | undefined) => (value !== null && value !== undefined) ? `${value.toFixed(2)}%` : 'N/A'
    },
    { 
      id: 'subProductStatus', 
      label: 'Status', 
      minWidth: 100,
      format: (value: SubProductStatus | null | undefined) => (
        <StatusBadge status={value || 'UNKNOWN'} />
      )
    },
    { 
      id: 'verified', 
      label: 'Verification', 
      minWidth: 100,
      format: (value: boolean | null | undefined) => (
        <StatusBadge status={value === true ? 'VERIFIED' : 'PENDING'} />
      )
    },
    { 
      id: 'actions', 
      label: 'Actions', 
      minWidth: 120,
      format: (_: any, row: SubProductResponseDTO) => (
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="View Details">
            <IconButton 
              component={RouterLink} 
              to={`/subproducts/view/${row.subProductId}`} 
              color="info"
              size="small"
            >
              <ViewIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Edit">
            <IconButton 
              component={RouterLink} 
              to={`/subproducts/edit/${row.subProductId}`} 
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
                onClick={() => handleOpenVerifyModal(row.subProductId)}
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
        title="SubProduct Management"
        buttonText="Add SubProduct"
        buttonLink="/subproducts/new"
        startIcon={<AddIcon />}
      />

      {/* Search Panel - Right aligned */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 3 }}>
        <TextField
          value={searchTerm}
          onChange={handleSearchInputChange}
          placeholder="Search subproducts..."
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
          title="Error Loading SubProducts" 
          onRetry={refetch}
        />
      ) : (
        <DataTable
          columns={columns}
          rows={paginatedData}
          totalItems={filteredSubProducts.length}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={setRowsPerPage}
          onSort={handleSort}
          loading={isLoading}
          idField="subProductId"
        />
      )}

      <VerificationModal
        open={verificationModal.open}
        onClose={handleCloseVerifyModal}
        onVerify={handleVerify}
        title="Verify SubProduct"
        description="Please enter your user ID to verify this subproduct."
      />
    </Box>
  );
};

export default SubProductList;
