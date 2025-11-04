import { Box, Paper, Typography, type SxProps, type Theme } from '@mui/material';
import type { ReactNode } from 'react';

interface FormSectionProps {
  title: string;
  children: ReactNode;
  sx?: SxProps<Theme>;
  compact?: boolean;
}

const FormSection = ({ title, children, sx, compact = false }: FormSectionProps) => {
  return (
    <Paper sx={{ p: compact ? 2 : 3, mb: compact ? 2 : 3, ...sx }}>
      <Typography variant={compact ? "subtitle1" : "h6"} component="h2" gutterBottom sx={{ fontSize: compact ? '1rem' : undefined }}>
        {title}
      </Typography>
      <Box>
        {children}
      </Box>
    </Paper>
  );
};

export default FormSection;
