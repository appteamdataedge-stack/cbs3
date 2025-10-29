import { ArrowBack as ArrowBackIcon, Edit as EditIcon } from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  Grid,
  Typography
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useParams } from 'react-router-dom';
import { getCustomerById } from '../../api/customerService';
import { StatusBadge } from '../../components/common';
import { CustomerType } from '../../types';

const CustomerDetails = () => {
  const { id } = useParams<{ id: string }>();

  // Fetch customer details
  const { 
    data: customer, 
    isLoading, 
    error
  } = useQuery({
    queryKey: ['customer', id],
    queryFn: () => getCustomerById(Number(id)),
    enabled: !!id,
  });

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
        <Box sx={{ mb: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box>
              <Typography variant="h4" component="h1" gutterBottom>
                Customer Details
              </Typography>
              <Typography variant="subtitle1" color="text.secondary">
                View customer information
              </Typography>
            </Box>
            <Button
              component="a"
              href="/customers"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Customers
            </Button>
          </Box>
        </Box>
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to load customer details. Please try again.
        </Alert>
      </Box>
    );
  }

  if (!customer) {
    return (
      <Box sx={{ p: 3 }}>
        <Box sx={{ mb: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box>
              <Typography variant="h4" component="h1" gutterBottom>
                Customer Details
              </Typography>
              <Typography variant="subtitle1" color="text.secondary">
                View customer information
              </Typography>
            </Box>
            <Button
              component="a"
              href="/customers"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Customers
            </Button>
          </Box>
        </Box>
        <Alert severity="warning" sx={{ mt: 2 }}>
          Customer not found.
        </Alert>
      </Box>
    );
  }

  // Get customer name based on type
  const customerName = customer.custType === CustomerType.INDIVIDUAL
    ? `${customer.firstName || ''} ${customer.lastName || ''}`.trim()
    : customer.tradeName || '';

  return (
    <Box sx={{ p: 3 }}>
      <Box sx={{ mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Box>
            <Typography variant="h4" component="h1" gutterBottom>
              Customer Details
            </Typography>
            <Typography variant="subtitle1" color="text.secondary">
              Customer ID: {customer.custId}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              component="a"
              href="/customers"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Customers
            </Button>
            <Button
              component="a"
              href={`/customers/${customer.custId}`}
              variant="outlined"
              startIcon={<EditIcon />}
            >
              Edit Customer
            </Button>
          </Box>
        </Box>
      </Box>

      <Grid container spacing={3} sx={{ mt: 2 }}>
        {/* Basic Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Basic Information
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Customer ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {customer.custId}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    External Customer ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {customer.extCustId}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Customer Type
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {customer.custType}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Customer Name
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {customerName}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Mobile Number
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {customer.mobile || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Status
                  </Typography>
                  <StatusBadge status={customer.verified ? 'VERIFIED' : 'PENDING'} />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Individual Customer Details */}
        {customer.custType === CustomerType.INDIVIDUAL && (
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Individual Customer Details
                </Typography>
                <Divider sx={{ mb: 2 }} />
                
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">
                      First Name
                    </Typography>
                    <Typography variant="body1" fontWeight="medium">
                      {customer.firstName || 'N/A'}
                    </Typography>
                  </Grid>
                  
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">
                      Last Name
                    </Typography>
                    <Typography variant="body1" fontWeight="medium">
                      {customer.lastName || 'N/A'}
                    </Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        )}

        {/* Corporate Customer Details */}
        {customer.custType === CustomerType.CORPORATE && (
          <Grid item xs={12} md={6}>
            <Card>
              <CardContent>
                <Typography variant="h6" gutterBottom>
                  Corporate Customer Details
                </Typography>
                <Divider sx={{ mb: 2 }} />
                
                <Grid container spacing={2}>
                  <Grid item xs={6}>
                    <Typography variant="body2" color="text.secondary">
                      Trade Name
                    </Typography>
                    <Typography variant="body1" fontWeight="medium">
                      {customer.tradeName || 'N/A'}
                    </Typography>
                  </Grid>
                </Grid>
              </CardContent>
            </Card>
          </Grid>
        )}

        {/* Address Information */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Address Information
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Typography variant="body2" color="text.secondary">
                    Address
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {customer.address1 || 'N/A'}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default CustomerDetails;
