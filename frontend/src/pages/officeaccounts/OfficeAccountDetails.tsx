import { ArrowBack as ArrowBackIcon, Close as CloseIcon, Edit as EditIcon } from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  Grid,
  Typography
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { toast } from 'react-toastify';
import { closeOfficeAccount, getOfficeAccountByAccountNo } from '../../api/officeAccountService';
import { PageHeader, StatusBadge } from '../../components/common';
import { AccountStatus } from '../../types';

const OfficeAccountDetails = () => {
  const { accountNo } = useParams<{ accountNo: string }>();
  const queryClient = useQueryClient();
  const [closeConfirmDialog, setCloseConfirmDialog] = useState(false);

  // Fetch account details
  const { 
    data: account, 
    isLoading, 
    error,
    refetch
  } = useQuery({
    queryKey: ['officeAccount', accountNo],
    queryFn: () => getOfficeAccountByAccountNo(accountNo || ''),
    enabled: !!accountNo,
  });

  // Close account mutation
  const closeAccountMutation = useMutation({
    mutationFn: closeOfficeAccount,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['officeAccount', accountNo] });
      queryClient.invalidateQueries({ queryKey: ['officeAccounts'] });
      toast.success('Office account closed successfully');
      setCloseConfirmDialog(false);
      refetch();
    },
    onError: (error: Error) => {
      toast.error(`Failed to close office account: ${error.message}`);
      setCloseConfirmDialog(false);
    },
  });

  // Handle close account
  const handleOpenCloseDialog = () => {
    setCloseConfirmDialog(true);
  };

  const handleCloseDialog = () => {
    setCloseConfirmDialog(false);
  };

  const handleCloseAccount = () => {
    if (accountNo) {
      closeAccountMutation.mutate(accountNo);
    }
  };

  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <PageHeader 
          title="Office Account Details" 
          subtitle="View office account information"
          buttonText="Back to Office Accounts"
          buttonLink="/office-accounts"
          startIcon={<ArrowBackIcon />}
        />
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to load office account details. Please try again.
        </Alert>
      </Box>
    );
  }

  if (!account) {
    return (
      <Box sx={{ p: 3 }}>
        <PageHeader 
          title="Office Account Details" 
          subtitle="View office account information"
          buttonText="Back to Office Accounts"
          buttonLink="/office-accounts"
          startIcon={<ArrowBackIcon />}
        />
        <Alert severity="warning" sx={{ mt: 2 }}>
          Office account not found.
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box>
            <Typography variant="h4" component="h1" gutterBottom>
              Office Account Details
            </Typography>
            <Typography variant="subtitle1" color="text.secondary">
              Account Number: {account.accountNo}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              component="a"
              href="/office-accounts"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Office Accounts
            </Button>
            {account.accountStatus !== AccountStatus.CLOSED && (
              <>
                <Button
                  component="a"
                  href={`/office-accounts/edit/${account.accountNo}`}
                  variant="outlined"
                  startIcon={<EditIcon />}
                >
                  Edit Account
                </Button>
                <Button
                  onClick={handleOpenCloseDialog}
                  variant="outlined"
                  color="error"
                  startIcon={<CloseIcon />}
                >
                  Close Account
                </Button>
              </>
            )}
          </Box>
        </Box>
      </Box>

      <Grid container spacing={3} sx={{ mt: 2 }}>
        {/* Account Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Account Information
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Account Number
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {account.accountNo}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Account Name
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {account.acctName}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Branch Code
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {account.branchCode}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Account Status
                  </Typography>
                  <StatusBadge status={account.accountStatus} />
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Date of Opening
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {new Date(account.dateOpening).toLocaleDateString()}
                  </Typography>
                </Grid>
                
                {account.dateClosure && (
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">
                      Date of Closure
                    </Typography>
                    <Typography variant="body1" fontWeight="medium">
                      {new Date(account.dateClosure).toLocaleDateString()}
                    </Typography>
                  </Grid>
                )}
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Reconciliation Required
                  </Typography>
                  <StatusBadge 
                    status={account.reconciliationRequired ? 'Required' : 'Not Required'} 
                    statusMap={{
                      'Required': 'success',
                      'Not Required': 'default'
                    }}
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Sub Product Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Sub Product Information
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Sub Product ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {account.subProductId}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Sub Product Name
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {account.subProductName || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    GL Number
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {account.glNum || 'N/A'}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

      </Grid>

      {/* Close Account Confirmation Dialog */}
      <Dialog
        open={closeConfirmDialog}
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
            <strong>{account.accountNo}</strong> -{' '}
            <strong>{account.acctName}</strong>?
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

export default OfficeAccountDetails;
