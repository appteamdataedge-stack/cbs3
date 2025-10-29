import { Box, Paper, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface FormSectionProps {
  title: string;
  children: ReactNode;
}

const FormSection = ({ title, children }: FormSectionProps) => {
  return (
    <Paper sx={{ p: 3, mb: 3 }}>
      <Typography variant="h6" component="h2" gutterBottom>
        {title}
      </Typography>
      <Box>
        {children}
      </Box>
    </Paper>
  );
};

export default FormSection;
