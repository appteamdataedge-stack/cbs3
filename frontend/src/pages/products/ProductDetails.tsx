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
import { getProductById } from '../../api/productService';
import { StatusBadge } from '../../components/common';

const ProductDetails = () => {
  const { id } = useParams<{ id: string }>();

  // Fetch product details
  const { 
    data: product, 
    isLoading, 
    error
  } = useQuery({
    queryKey: ['product', id],
    queryFn: () => getProductById(Number(id)),
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
                Product Details
              </Typography>
              <Typography variant="subtitle1" color="text.secondary">
                View product information
              </Typography>
            </Box>
            <Button
              component="a"
              href="/products"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Products
            </Button>
          </Box>
        </Box>
        <Alert severity="error" sx={{ mt: 2 }}>
          Failed to load product details. Please try again.
        </Alert>
      </Box>
    );
  }

  if (!product) {
    return (
      <Box sx={{ p: 3 }}>
        <Box sx={{ mb: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Box>
              <Typography variant="h4" component="h1" gutterBottom>
                Product Details
              </Typography>
              <Typography variant="subtitle1" color="text.secondary">
                View product information
              </Typography>
            </Box>
            <Button
              component="a"
              href="/products"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Products
            </Button>
          </Box>
        </Box>
        <Alert severity="warning" sx={{ mt: 2 }}>
          Product not found.
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
              Product Details
            </Typography>
            <Typography variant="subtitle1" color="text.secondary">
              Product ID: {product.productId}
            </Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              component="a"
              href="/products"
              variant="outlined"
              startIcon={<ArrowBackIcon />}
            >
              Back to Products
            </Button>
            <Button
              component="a"
              href={`/products/${product.productId}`}
              variant="outlined"
              startIcon={<EditIcon />}
            >
              Edit Product
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
                    Product ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.productId}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Product Code
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.productCode || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={12}>
                  <Typography variant="body2" color="text.secondary">
                    Product Name
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.productName || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    GL Number
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.cumGLNum || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Status
                  </Typography>
                  <StatusBadge status={product.verified ? 'VERIFIED' : 'PENDING'} />
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
                    {product.makerId || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Entry Date
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.entryDate ? new Date(product.entryDate).toLocaleDateString() : 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Entry Time
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.entryTime || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Verifier ID
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.verifierId || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Verification Date
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.verificationDate ? new Date(product.verificationDate).toLocaleDateString() : 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={6}>
                  <Typography variant="body2" color="text.secondary">
                    Verification Time
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.verificationTime || 'N/A'}
                  </Typography>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Product Information */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>
                Product Information
              </Typography>
              <Divider sx={{ mb: 2 }} />
              
              <Grid container spacing={2}>
                <Grid item xs={12} md={4}>
                  <Typography variant="body2" color="text.secondary">
                    Product Code
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.productCode || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={12} md={4}>
                  <Typography variant="body2" color="text.secondary">
                    Product Name
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.productName || 'N/A'}
                  </Typography>
                </Grid>
                
                <Grid item xs={12} md={4}>
                  <Typography variant="body2" color="text.secondary">
                    Cumulative GL Number
                  </Typography>
                  <Typography variant="body1" fontWeight="medium">
                    {product.cumGLNum || 'N/A'}
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

export default ProductDetails;
