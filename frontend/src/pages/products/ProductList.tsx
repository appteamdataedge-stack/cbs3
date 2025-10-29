import { Add as AddIcon, Edit as EditIcon, Verified as VerifiedIcon, Search as SearchIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { Box, IconButton, Tooltip, TextField, InputAdornment } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState, useMemo, useEffect } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAllProducts, verifyProduct } from '../../api/productService';
import { DataTable, PageHeader, StatusBadge, VerificationModal, ErrorDisplay } from '../../components/common';
import type { Column } from '../../components/common';
import type { CustomerVerificationDTO, ProductResponseDTO } from '../../types';

const ProductList = () => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [, setSort] = useState<string | undefined>(undefined);
  const [searchTerm, setSearchTerm] = useState('');
  const [verificationModal, setVerificationModal] = useState<{
    open: boolean;
    productId: number | null;
  }>({
    open: false,
    productId: null,
  });

  // Fetch all products at once
  const { data: allProducts, isLoading, error, refetch } = useQuery({
    queryKey: ['products', 'all'],
    queryFn: () => getAllProducts(0, 1000), // Get a large number to effectively get all
    retry: 3,
    retryDelay: 1000
  });
  
  // Filter products based on search term
  const filteredProducts = useMemo(() => {
    if (!allProducts?.content || searchTerm.trim() === '') {
      return allProducts?.content || [];
    }
    
    const lowerCaseSearch = searchTerm.toLowerCase();
    
    return allProducts.content.filter((product) => {
      // Search in various fields
      return (
        product.productName?.toLowerCase().includes(lowerCaseSearch) || 
        product.productCode?.toLowerCase().includes(lowerCaseSearch) ||
        String(product.productId).includes(lowerCaseSearch) ||
        (product.makerId && product.makerId.toLowerCase().includes(lowerCaseSearch)) ||
        (product.cumGLNum && product.cumGLNum.toLowerCase().includes(lowerCaseSearch))
      );
    });
  }, [allProducts, searchTerm]);
  
  // Paginated data for the table
  const paginatedData = useMemo(() => {
    const startIndex = page * rowsPerPage;
    const endIndex = startIndex + rowsPerPage;
    return filteredProducts.slice(startIndex, endIndex);
  }, [filteredProducts, page, rowsPerPage]);
  
  // Reset to first page when search changes
  useEffect(() => {
    setPage(0);
  }, [searchTerm]);

  // Handle error if needed
  if (error) {
    console.error('Error fetching products:', error);
  }

  // Handle search input change - dynamic search as user types
  const handleSearchInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  };

  // Handle sort
  const handleSort = (field: string, direction: 'asc' | 'desc') => {
    setSort(`${field},${direction}`);
  };

  // Handle verify
  const handleOpenVerifyModal = (productId: number) => {
    setVerificationModal({
      open: true,
      productId,
    });
  };

  const handleCloseVerifyModal = () => {
    setVerificationModal({
      open: false,
      productId: null,
    });
  };

  const handleVerify = async (verifierId: string) => {
    if (!verificationModal.productId) return;

    try {
      const verificationData: CustomerVerificationDTO = { verifierId };
      await verifyProduct(verificationModal.productId, verificationData);
      toast.success('Product verified successfully');
      refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to verify product');
      throw err; // Re-throw to let the modal component handle the error
    }
  };

  // Table columns
  const columns: Column<ProductResponseDTO>[] = [
    { id: 'productId', label: 'ID', minWidth: 50, sortable: true },
    { id: 'productCode', label: 'Product Code', minWidth: 120, sortable: true },
    { id: 'productName', label: 'Product Name', minWidth: 200, sortable: true },
    { id: 'cumGLNum', label: 'GL Number', minWidth: 150 },
    { id: 'makerId', label: 'Created By', minWidth: 120 },
    { 
      id: 'entryDate', 
      label: 'Created Date', 
      minWidth: 120,
      format: (value: string) => new Date(value).toLocaleDateString()
    },
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
      format: (_: any, row: ProductResponseDTO) => (
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="View Details">
            <IconButton 
              component={RouterLink} 
              to={`/products/view/${row.productId}`} 
              color="info"
              size="small"
            >
              <ViewIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Edit">
            <IconButton 
              component={RouterLink} 
              to={`/products/edit/${row.productId}`} 
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
                onClick={() => handleOpenVerifyModal(row.productId)}
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
        title="Product Management"
        buttonText="Add Product"
        buttonLink="/products/new"
        startIcon={<AddIcon />}
      />

      {/* Search Panel - Right aligned */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 3 }}>
        <TextField
          value={searchTerm}
          onChange={handleSearchInputChange}
          placeholder="Search products..."
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
          title="Error Loading Products" 
          onRetry={refetch}
        />
      ) : (
        <DataTable
          columns={columns}
          rows={paginatedData}
          totalItems={filteredProducts.length}
          page={page}
          rowsPerPage={rowsPerPage}
          onPageChange={setPage}
          onRowsPerPageChange={setRowsPerPage}
          onSort={handleSort}
          loading={isLoading}
          idField="productId"
        />
      )}

      <VerificationModal
        open={verificationModal.open}
        onClose={handleCloseVerifyModal}
        onVerify={handleVerify}
        title="Verify Product"
        description="Please enter your user ID to verify this product."
      />
    </Box>
  );
};

export default ProductList;
