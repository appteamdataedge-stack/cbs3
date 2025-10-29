import {
  AccountBalance as AccountIcon,
  Group as CustomerIcon,
  ViewModule as ProductIcon
} from '@mui/icons-material';
import { Box, Card, CardContent, CircularProgress, Grid, Paper, Typography } from '@mui/material';
import { PageHeader } from '../components/common';
import { useDashboardSummary } from '../utils/dashboardUtils';

interface StatCardProps {
  title: string;
  count: number;
  icon: React.ReactNode;
  color: string;
}

const StatCard = ({ title, count, icon, color }: StatCardProps) => (
  <Card raised sx={{ width: '100%', height: '100%' }}>
    <CardContent>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box>
          <Typography variant="subtitle2" color="text.secondary">
            {title}
          </Typography>
          <Typography variant="h4" component="div" sx={{ mt: 1 }}>
            {count.toLocaleString()}
          </Typography>
        </Box>
        <Box
          sx={{
            backgroundColor: color,
            width: 48,
            height: 48,
            borderRadius: '50%',
            display: 'flex',
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
          {icon}
        </Box>
      </Box>
    </CardContent>
  </Card>
);

const Dashboard = () => {
  const { customerCount, productCount, subProductCount, accountCount, isLoading, error } = useDashboardSummary();

  return (
    <Box sx={{ width: '100%' }}>
      <PageHeader title="Dashboard" />

      {isLoading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : error ? (
        <Paper sx={{ p: 3, backgroundColor: '#fff9fa', borderLeft: '4px solid #f44336' }}>
          <Typography color="error">
            Error loading dashboard data. Please refresh the page.
          </Typography>
        </Paper>
      ) : (
        <Grid container spacing={3} sx={{ width: '100%' }}>
          <Grid item xs={12} sm={6} md={3} sx={{ display: 'flex' }}>
            <StatCard
              title="Customers"
              count={customerCount}
              icon={<CustomerIcon sx={{ color: 'white' }} />}
              color="#1976d2"
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3} sx={{ display: 'flex' }}>
            <StatCard
              title="Products"
              count={productCount}
              icon={<ProductIcon sx={{ color: 'white' }} />}
              color="#2e7d32"
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3} sx={{ display: 'flex' }}>
            <StatCard
              title="SubProducts"
              count={subProductCount}
              icon={<ProductIcon sx={{ color: 'white' }} />}
              color="#ff9800"
            />
          </Grid>
          <Grid item xs={12} sm={6} md={3} sx={{ display: 'flex' }}>
            <StatCard
              title="Accounts"
              count={accountCount}
              icon={<AccountIcon sx={{ color: 'white' }} />}
              color="#9c27b0"
            />
          </Grid>
        </Grid>
      )}
    </Box>
  );
};

export default Dashboard;
