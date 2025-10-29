import { Chip } from '@mui/material';

interface TransactionStatusChipProps {
  status: string;
}

export const TransactionStatusChip = ({ status }: TransactionStatusChipProps) => {
  const getStatusColor = () => {
    switch (status) {
      case 'Entry':
        return 'warning';
      case 'Posted':
        return 'info';
      case 'Verified':
        return 'success';
      default:
        return 'default';
    }
  };

  return (
    <Chip 
      label={status} 
      color={getStatusColor() as any}
      size="small"
      sx={{ minWidth: 80 }}
    />
  );
};

