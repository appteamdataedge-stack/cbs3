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
import { getSubProductById } from '../../api/subProductService';
import { StatusBadge } from '../../components/common';

const SubProductDetails = () => {
  const { id } = useParams<{ id: string }>();

  // Fetch subproduct details
  const { 
    data: subProduct, 
    isLoading, 
    error
  } = useQuery({
    queryKey: ['subproduct', id],
    queryFn: () => getSubProductById(Number(id)),
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
                Sub Product Details
              </Typography>
              <Typography variant="subtitle1" color="text.secondary">
                View sub product information
              </Typography>
            </Box>
            <Button
              component="a"
              href="/subproducts"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Sub Products
            </Button>
          </Box>
        </Box>
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to load sub product details. Please try again.
        </Alert>
      </Box>
    );
  }

  if (!subProduct) {
    return (
      <Box sx={{ p: 3 }}>
        <Box sx={{ mb: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box>
              <Typography variant="h4" component="h1" gutterBottom>
                Sub Product Details
              </Typography>
              <Typography variant="subtitle1" color="text.secondary">
                View sub product information
              </Typography>
            </Box>
            <Button
              component="a"
              href="/subproducts"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Sub Products
            </Button>
          </Box>
        </Box>
        <Alert severity="warning" sx={{ mt: 2 }}>
          Sub Product not found.
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
              Sub Product Details
            </Typography>
            <Typography variant="subtitle1" color="text.secondary">
              Sub Product ID: {subProduct.subProductId}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              component="a"
              href="/subproducts"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Sub Products
            </Button>
            <Button
              component="a"
              href={`/subproducts/${subProduct.subProductId}`}
              variant="outlined"
              startIcon={<EditIcon />}
            >
              Edit Sub Product
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
                    Sub Product ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.subProductId}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Product ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.productId}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Sub Product Code
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.subProductCode || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Sub Product Name
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.subProductName || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Interest Code
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.inttCode || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Status
                  </Typography>
                  <StatusBadge status={subProduct.subProductStatus} />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* GL Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                GL Information
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Cumulative GL Number
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.cumGLNum || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    External GL Number
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.extGLNum || 'N/A'}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Audit Information */}
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Audit Information
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <Grid container spacing={2}>
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Maker ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.makerId || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Entry Date
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.entryDate ? new Date(subProduct.entryDate).toLocaleDateString() : 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Entry Time
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.entryTime || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Verifier ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.verifierId || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Verification Date
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.verificationDate ? new Date(subProduct.verificationDate).toLocaleDateString() : 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Verification Time
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.verificationTime || 'N/A'}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Sub Product Information */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Sub Product Information
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <Grid container spacing={2}>
                <Grid item xs={12} md={3}>
                  <Typography variant="body2" color="text.secondary">
                    Sub Product Code
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.subProductCode || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={12} md={3}>
                  <Typography variant="body2" color="text.secondary">
                    Sub Product Name
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.subProductName || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={12} md={3}>
                  <Typography variant="body2" color="text.secondary">
                    Interest Code
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {subProduct.inttCode || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={12} md={3}>
                  <Typography variant="body2" color="text.secondary">
                    Status
                  </Typography>
                  <StatusBadge status={subProduct.subProductStatus} />
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </Box>
  );
};

export default SubProductDetails;
