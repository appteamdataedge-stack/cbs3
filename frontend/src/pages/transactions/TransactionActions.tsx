import { CheckCircle as PostIcon, VerifiedUser as VerifyIcon, Undo as ReverseIcon, Visibility as ViewIcon } from '@mui/icons-material';
import { Box, IconButton, Tooltip, CircularProgress } from '@mui/material';
import type { TransactionResponseDTO } from '../../types';

interface TransactionActionsProps {
  transaction: TransactionResponseDTO;
  onView: (transaction: TransactionResponseDTO) => void;
  onPost: (tranId: string) => void;
  onVerify: (tranId: string) => void;
  onReverse: (tranId: string) => void;
  isLoading?: boolean;
}

export const TransactionActions = ({ 
  transaction, 
  onView, 
  onPost, 
  onVerify, 
  onReverse,
  isLoading = false 
}: TransactionActionsProps) => {
  const canPost = transaction.status === 'Entry';
  const canVerify = transaction.status === 'Posted';
  const canReverse = transaction.status === 'Posted' || transaction.status === 'Verified';

  return (
    <Box sx={{ display: 'flex', gap: 0.5 }}>
      {/* View Details */}
      <Tooltip title="View Details">
        <IconButton 
          color="primary"
          size="small"
          onClick={() => onView(transaction)}
          disabled={isLoading}
        >
          <ViewIcon fontSize="small" />
        </IconButton>
      </Tooltip>

      {/* Post Transaction (Entry → Posted) */}
      {canPost && (
        <Tooltip title="Post Transaction (Checker Approval)">
          <IconButton 
            color="success"
            size="small"
            onClick={() => onPost(transaction.tranId)}
            disabled={isLoading}
          >
            {isLoading ? <CircularProgress size={16} /> : <PostIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
      )}

      {/* Verify Transaction (Posted → Verified) */}
      {canVerify && (
        <Tooltip title="Verify Transaction (Final Approval)">
          <IconButton 
            color="info"
            size="small"
            onClick={() => onVerify(transaction.tranId)}
            disabled={isLoading}
          >
            {isLoading ? <CircularProgress size={16} /> : <VerifyIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
      )}

      {/* Reverse Transaction */}
      {canReverse && (
        <Tooltip title="Reverse Transaction">
          <IconButton 
            color="warning"
            size="small"
            onClick={() => onReverse(transaction.tranId)}
            disabled={isLoading}
          >
            {isLoading ? <CircularProgress size={16} /> : <ReverseIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
      )}
    </Box>
  );
};

