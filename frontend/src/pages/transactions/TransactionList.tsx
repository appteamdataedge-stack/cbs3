import { Add as AddIcon, Visibility as ViewIcon, Search as SearchIcon, Verified as VerifiedIcon } from '@mui/icons-material';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  Grid,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
  TextField,
  InputAdornment
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { getAllTransactions, getTransactionById, verifyTransaction } from '../../api/transactionService';
import { DataTable, PageHeader, StatusBadge, VerificationModal } from '../../components/common';
import type { Column } from '../../components/common';
import type { TransactionLineResponseDTO, TransactionResponseDTO } from '../../types';
import { DrCrFlag } from '../../types';

const TransactionList = () => {
  const { tranId } = useParams<{ tranId: string }>();
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedTransaction, setSelectedTransaction] = useState<TransactionResponseDTO | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [verificationModal, setVerificationModal] = useState<{
    open: boolean;
    tranId: string | null;
  }>({
    open: false,
    tranId: null,
  });

  // Fetch all transactions with pagination from backend
  const { data: transactionsData, isLoading, refetch } = useQuery({
    queryKey: ['transactions', page, rowsPerPage],
    queryFn: () => getAllTransactions(page, rowsPerPage),
  });

  // Fetch transaction by ID if provided in route params
  const { data: transactionData } = useQuery({
    queryKey: ['transaction', tranId],
    queryFn: () => getTransactionById(tranId || ''),
    enabled: !!tranId
  });

  // Get transactions from API response
  const transactions = transactionsData?.content || [];

  // Mocked transaction data (fallback for development - will be replaced by real data)
  const mockedTransactions: TransactionResponseDTO[] = [
    {
      tranId: 'TRX123456',
      tranDate: '2025-01-15',
      valueDate: '2025-01-15',
      narration: 'Fund transfer between accounts',
      balanced: true,
      status: 'Entry',
      lines: [
        {
          tranId: 'TRX123456-1',
          accountNo: 'ACC10001',
          accountName: 'John Doe Savings',
          drCrFlag: DrCrFlag.D,
          tranCcy: 'BDT',
          fcyAmt: 50000.00,
          exchangeRate: 1.0,
          lcyAmt: 50000.00,
          udf1: 'Transfer to investment'
        },
        {
          tranId: 'TRX123456-2',
          accountNo: 'ACC10002',
          accountName: 'John Doe Investment',
          drCrFlag: DrCrFlag.C,
          tranCcy: 'BDT',
          fcyAmt: 50000.00,
          exchangeRate: 1.0,
          lcyAmt: 50000.00,
          udf1: 'Transfer from savings'
        }
      ]
    },
    {
      tranId: 'TRX123457',
      tranDate: '2025-01-14',
      valueDate: '2025-01-14',
      narration: 'Deposit to account',
      balanced: true,
      status: 'Posted',
      lines: [
        {
          tranId: 'TRX123457-1',
          accountNo: 'ACC10003',
          accountName: 'Sarah Smith Savings',
          drCrFlag: DrCrFlag.C,
          tranCcy: 'BDT',
          fcyAmt: 25000.00,
          exchangeRate: 1.0,
          lcyAmt: 25000.00,
          udf1: 'Cash deposit'
        },
        {
          tranId: 'TRX123457-2',
          accountNo: 'CASH001',
          accountName: 'Cash Account',
          drCrFlag: DrCrFlag.D,
          tranCcy: 'BDT',
          fcyAmt: 25000.00,
          exchangeRate: 1.0,
          lcyAmt: 25000.00,
          udf1: 'Cash deposit'
        }
      ]
    },
    {
      tranId: 'TRX123458',
      tranDate: '2025-01-13',
      valueDate: '2025-01-13',
      narration: 'Loan disbursement',
      balanced: true,
      status: 'Verified',
      lines: [
        {
          tranId: 'TRX123458-1',
          accountNo: 'ACC10004',
          accountName: 'Ahmed Hassan Current',
          drCrFlag: DrCrFlag.C,
          tranCcy: 'BDT',
          fcyAmt: 100000.00,
          exchangeRate: 1.0,
          lcyAmt: 100000.00,
          udf1: 'Personal loan disbursement'
        },
        {
          tranId: 'TRX123458-2',
          accountNo: 'LOAN001',
          accountName: 'Personal Loan Portfolio',
          drCrFlag: DrCrFlag.D,
          tranCcy: 'BDT',
          fcyAmt: 100000.00,
          exchangeRate: 1.0,
          lcyAmt: 100000.00,
          udf1: 'Loan disbursement'
        }
      ]
    },
    {
      tranId: 'TRX123459',
      tranDate: '2025-01-12',
      valueDate: '2025-01-12',
      narration: 'Salary payment',
      balanced: true,
      status: 'Posted',
      lines: [
        {
          tranId: 'TRX123459-1',
          accountNo: 'ACC10005',
          accountName: 'Fatima Khatun Salary',
          drCrFlag: DrCrFlag.C,
          tranCcy: 'BDT',
          fcyAmt: 75000.00,
          exchangeRate: 1.0,
          lcyAmt: 75000.00,
          udf1: 'Monthly salary payment'
        },
        {
          tranId: 'TRX123459-2',
          accountNo: 'PAY001',
          accountName: 'Payroll Account',
          drCrFlag: DrCrFlag.D,
          tranCcy: 'BDT',
          fcyAmt: 75000.00,
          exchangeRate: 1.0,
          lcyAmt: 75000.00,
          udf1: 'Salary payment'
        }
      ]
    },
    {
      tranId: 'TRX123460',
      tranDate: '2025-01-11',
      valueDate: '2025-01-11',
      narration: 'Utility bill payment',
      balanced: true,
      status: 'Entry',
      lines: [
        {
          tranId: 'TRX123460-1',
          accountNo: 'ACC10006',
          accountName: 'Rahman Electric Bill',
          drCrFlag: DrCrFlag.D,
          tranCcy: 'BDT',
          fcyAmt: 15000.00,
          exchangeRate: 1.0,
          lcyAmt: 15000.00,
          udf1: 'Electricity bill payment'
        },
        {
          tranId: 'TRX123460-2',
          accountNo: 'UTIL001',
          accountName: 'Utility Collection Account',
          drCrFlag: DrCrFlag.C,
          tranCcy: 'BDT',
          fcyAmt: 15000.00,
          exchangeRate: 1.0,
          lcyAmt: 15000.00,
          udf1: 'Electricity bill collection'
        }
      ]
    }
  ];
  
  // Use real data from API, fallback to mock data if API returns empty
  const dataToUse = transactions.length > 0 ? transactions : mockedTransactions;

  // Filter transactions based on search term
  const filteredTransactions = useMemo(() => {
    if (!dataToUse || searchTerm.trim() === '') {
      return dataToUse || [];
    }
    
    const lowerCaseSearch = searchTerm.toLowerCase();
    
    return dataToUse.filter((transaction) => {
      // Search in various fields
      return (
        transaction.tranId.toLowerCase().includes(lowerCaseSearch) || 
        (transaction.narration && transaction.narration.toLowerCase().includes(lowerCaseSearch)) ||
        transaction.status.toLowerCase().includes(lowerCaseSearch) ||
        transaction.valueDate.includes(lowerCaseSearch) ||
        transaction.tranDate.includes(lowerCaseSearch)
      );
    });
  }, [searchTerm, dataToUse]);
  
  // Use filtered transactions directly (backend handles pagination)
  const paginatedData = filteredTransactions;
  
  // Total count from API or filtered results
  const totalItems = transactionsData?.totalElements || filteredTransactions.length;

  // Handle transaction data when it's loaded (from route params)
  if (transactionData && !selectedTransaction && dialogOpen === false) {
    setSelectedTransaction(transactionData);
    setDialogOpen(true);
  }

  // Open transaction details dialog
  const handleOpenDialog = (transaction: TransactionResponseDTO) => {
    setSelectedTransaction(transaction);
    setDialogOpen(true);
  };

  // Close transaction details dialog
  const handleCloseDialog = () => {
    setDialogOpen(false);
  };

  // Handle search input change - dynamic search as user types
  const handleSearchInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
  };

  // Handle opening verification modal
  const handleOpenVerifyModal = (tranId: string) => {
    setVerificationModal({
      open: true,
      tranId,
    });
  };

  // Handle closing verification modal
  const handleCloseVerifyModal = () => {
    setVerificationModal({
      open: false,
      tranId: null,
    });
  };

  // Handle transaction verification
  const handleVerify = async (_verifierId: string) => {
    if (!verificationModal.tranId) return;

    try {
      await verifyTransaction(verificationModal.tranId);
      toast.success('Transaction verified successfully');
      // Refetch transactions to update the status
      refetch();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : 'Failed to verify transaction');
      throw err; // Re-throw to let the modal component handle the error
    }
  };


  // Format date
  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString();
  };

  // Table columns
  const columns: Column<TransactionResponseDTO>[] = [
    { id: 'tranId', label: 'Transaction ID', minWidth: 150, sortable: true },
    { 
      id: 'valueDate', 
      label: 'Value Date', 
      minWidth: 120,
      format: (value: string) => formatDate(value),
      sortable: true
    },
    { 
      id: 'tranDate', 
      label: 'Transaction Date', 
      minWidth: 120,
      format: (value: string) => formatDate(value)
    },
    { id: 'narration', label: 'Description', minWidth: 200 },
    { 
      id: 'status', 
      label: 'Status', 
      minWidth: 100,
      format: (value: string) => {
        // Map transaction statuses to StatusBadge statuses
        let status = 'PENDING';
        if (value === 'Verified') status = 'VERIFIED';
        else if (value === 'Posted') status = 'IN_PROGRESS';
        else if (value === 'Entry') status = 'ENTRY'; // Use ENTRY to display as "Entry"
        
        return <StatusBadge status={status} />;
      }
    },
    { 
      id: 'actions', 
      label: 'Actions', 
      minWidth: 120,
      format: (_: any, row: TransactionResponseDTO) => (
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="View Details">
            <IconButton 
              color="info"
              onClick={() => handleOpenDialog(row)}
              size="small"
            >
              <ViewIcon />
            </IconButton>
          </Tooltip>
          {row.status !== 'Verified' && (
            <Tooltip title="Verify">
              <IconButton 
                color="success" 
                onClick={() => handleOpenVerifyModal(row.tranId)}
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
        title="Transactions"
        buttonText="New Transaction"
        buttonLink="/transactions/new"
        startIcon={<AddIcon />}
      />

      {/* Search Panel - Right aligned */}
      <Box sx={{ display: 'flex', justifyContent: 'flex-end', mb: 3 }}>
        <TextField
          value={searchTerm}
          onChange={handleSearchInputChange}
          placeholder="Search transactions..."
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

      {/* Transaction History */}
      <Card sx={{ mb: 4 }}>
        <CardContent>
          <DataTable
            columns={columns}
            rows={paginatedData}
            totalItems={totalItems}
            page={page}
            rowsPerPage={rowsPerPage}
            onPageChange={setPage}
            onRowsPerPageChange={setRowsPerPage}
            loading={isLoading}
            idField="tranId"
            emptyContent={
              <TableCell colSpan={6} align="center">
                {isLoading ? 'Loading transactions...' : 'No transactions found. Create your first transaction.'}
              </TableCell>
            }
          />
        </CardContent>
      </Card>

      {/* Transaction Details Dialog */}
      <Dialog
        open={dialogOpen}
        onClose={handleCloseDialog}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          <Box display="flex" justifyContent="space-between" alignItems="center">
            <Typography variant="h6">
              Transaction {selectedTransaction?.tranId}
            </Typography>
            <Button onClick={handleCloseDialog}>Close</Button>
          </Box>
        </DialogTitle>
        <DialogContent>
          {selectedTransaction && (
            <>
              <Grid container spacing={2} sx={{ mb: 3 }}>
                <Grid item xs={12} md={4}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Transaction ID
                  </Typography>
                  <Typography variant="body1">
                    {selectedTransaction.tranId}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Value Date
                  </Typography>
                  <Typography variant="body1">
                    {formatDate(selectedTransaction.valueDate)}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Transaction Date
                  </Typography>
                  <Typography variant="body1">
                    {formatDate(selectedTransaction.tranDate)}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={8}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Narration
                  </Typography>
                  <Typography variant="body1">
                    {selectedTransaction.narration || '-'}
                  </Typography>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Typography variant="subtitle2" color="text.secondary">
                    Status
                  </Typography>
                  <Typography variant="body1">
                    {selectedTransaction.status}
                  </Typography>
                </Grid>
              </Grid>

              <Divider sx={{ my: 2 }} />
              
              <Typography variant="subtitle1" sx={{ mt: 2, mb: 1 }}>
                Transaction Lines
              </Typography>

              <TableContainer component={Paper}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Account</TableCell>
                      <TableCell>Dr/Cr</TableCell>
                      <TableCell>Currency</TableCell>
                      <TableCell align="right">FCY Amount</TableCell>
                      <TableCell align="right">Exchange Rate</TableCell>
                      <TableCell align="right">LCY Amount</TableCell>
                      <TableCell>Narration</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {selectedTransaction.lines.map((line: TransactionLineResponseDTO) => (
                      <TableRow key={line.tranId}>
                        <TableCell>{line.accountNo} {line.accountName && `(${line.accountName})`}</TableCell>
                        <TableCell>
                          <Chip 
                            label={line.drCrFlag === DrCrFlag.D ? 'Debit' : 'Credit'} 
                            color={line.drCrFlag === DrCrFlag.D ? 'primary' : 'secondary'} 
                            size="small"
                          />
                        </TableCell>
                        <TableCell>{line.tranCcy}</TableCell>
                        <TableCell align="right">{line.fcyAmt.toLocaleString()}</TableCell>
                        <TableCell align="right">{line.exchangeRate}</TableCell>
                        <TableCell align="right">{line.lcyAmt.toLocaleString()}</TableCell>
                        <TableCell>{line.udf1 || '-'}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              {/* Totals */}
              <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
                <Typography variant="subtitle1">
                  Transaction Status: {selectedTransaction.status} | Balanced: {selectedTransaction.balanced ? 'Yes' : 'No'}
                </Typography>
              </Box>
            </>
          )}
        </DialogContent>
      </Dialog>

      {/* Verification Modal */}
      <VerificationModal
        open={verificationModal.open}
        onClose={handleCloseVerifyModal}
        onVerify={handleVerify}
        title="Verify Transaction"
        description="Please enter your verifier ID to verify this transaction."
      />
    </Box>
  );
};

export default TransactionList;
