import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField
} from '@mui/material';
import { useState } from 'react';

interface VerificationModalProps {
  open: boolean;
  onClose: () => void;
  onVerify: (verifierId: string) => Promise<void>;
  title?: string;
  description?: string;
}

const VerificationModal = ({
  open,
  onClose,
  onVerify,
  title = 'Verify',
  description = 'Please enter your user ID to verify this record.'
}: VerificationModalProps) => {
  const [verifierId, setVerifierId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleVerify = async () => {
    if (!verifierId) {
      setError('Verifier ID is required');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await onVerify(verifierId);
      setVerifierId('');
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Verification failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{description}</DialogContentText>
        <TextField
          autoFocus
          margin="dense"
          id="verifierId"
          label="Verifier ID"
          type="text"
          fullWidth
          variant="outlined"
          value={verifierId}
          onChange={(e) => setVerifierId(e.target.value)}
          error={Boolean(error)}
          helperText={error}
          disabled={loading}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={loading}>Cancel</Button>
        <Button onClick={handleVerify} variant="contained" disabled={loading}>
          {loading ? 'Verifying...' : 'Verify'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default VerificationModal;
