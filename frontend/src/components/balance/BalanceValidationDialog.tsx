/**
 * Balance Validation Dialog Component
 * 
 * Shows validation results for debit transactions with detailed information
 */

import React from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Typography,
  Box,
  Alert,
  Divider,
  Grid,
  Chip
} from '@mui/material';
import type {
  AccountBalance,
  DebitValidationResult
} from '../../services/balanceValidationService';
import {
  formatBalance,
  formatAccountType,
  getAccountTypeColor
} from '../../services/balanceValidationService';

interface BalanceValidationDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm?: () => void;
  validationResult: DebitValidationResult | null;
  accountBalance: AccountBalance | null;
  showConfirmButton?: boolean;
}

export const BalanceValidationDialog: React.FC<BalanceValidationDialogProps> = ({
  open,
  onClose,
  onConfirm,
  validationResult,
  accountBalance,
  showConfirmButton = false
}) => {
  if (!validationResult || !accountBalance) {
    return null;
  }

  const { isValid, message, availableBalance, requestedAmount } = validationResult;
  const { accountNo, accountType, isOverdraftAccount } = accountBalance;

  const balanceAfterTransaction = availableBalance - requestedAmount;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box display="flex" alignItems="center" gap={2}>
          <Typography variant="h6">
            Transaction Validation
          </Typography>
          <Chip
            label={isValid ? 'Valid' : 'Invalid'}
            color={isValid ? 'success' : 'error'}
            size="small"
          />
        </Box>
      </DialogTitle>

      <DialogContent>
        <Box sx={{ mb: 3 }}>
          <Typography variant="body2" color="text.secondary">
            Account: {accountNo}
          </Typography>
          <Box display="flex" gap={1} alignItems="center" sx={{ mt: 1 }}>
            <Typography variant="body2">
              Type:
            </Typography>
            <Chip
              label={formatAccountType(accountType)}
              size="small"
              style={{ 
                backgroundColor: getAccountTypeColor(accountType),
                color: 'white'
              }}
            />
            {isOverdraftAccount && (
              <Chip
                label="Overdraft"
                size="small"
                color="warning"
              />
            )}
          </Box>
        </Box>

        {!isValid && message && (
          <Alert severity="error" sx={{ mb: 3 }}>
            {message}
          </Alert>
        )}

        {isValid && (
          <Alert severity="success" sx={{ mb: 3 }}>
            Transaction is valid and can proceed.
          </Alert>
        )}

        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Current Balance
              </Typography>
              <Typography variant="h6" color="primary">
                {formatBalance(availableBalance)}
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={12} md={6}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Requested Amount
              </Typography>
              <Typography variant="h6" color="secondary">
                {formatBalance(requestedAmount)}
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={12}>
            <Divider sx={{ my: 2 }} />
          </Grid>

          <Grid item xs={12} md={6}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Balance After Transaction
              </Typography>
              <Typography 
                variant="h6" 
                color={balanceAfterTransaction < 0 ? 'error' : 'primary'}
              >
                {formatBalance(balanceAfterTransaction)}
              </Typography>
            </Box>
          </Grid>

          <Grid item xs={12} md={6}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Transaction Status
              </Typography>
              <Chip
                label={isValid ? 'Approved' : 'Rejected'}
                color={isValid ? 'success' : 'error'}
                variant="outlined"
              />
            </Box>
          </Grid>
        </Grid>

        {accountType === 'LIABILITY' && (
          <Alert severity="info" sx={{ mt: 3 }}>
            Liability accounts cannot have negative balances.
          </Alert>
        )}

        {accountType === 'ASSET' && !isOverdraftAccount && (
          <Alert severity="info" sx={{ mt: 3 }}>
            Asset accounts cannot have negative balances unless they are overdraft/credit accounts.
          </Alert>
        )}

        {isOverdraftAccount && (
          <Alert severity="warning" sx={{ mt: 3 }}>
            This is an overdraft account and can have negative balances.
          </Alert>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} color="primary">
          Close
        </Button>
        {showConfirmButton && isValid && onConfirm && (
          <Button onClick={onConfirm} color="primary" variant="contained">
            Confirm Transaction
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};
