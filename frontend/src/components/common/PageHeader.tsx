import { Box, Button, Typography, Breadcrumbs, Link, useTheme, useMediaQuery, Paper, Divider } from '@mui/material';
import type { ReactNode } from 'react';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import HomeIcon from '@mui/icons-material/Home';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';

interface PageHeaderProps {
  title: string;
  buttonText?: string;
  buttonLink?: string;
  buttonAction?: () => void;
  startIcon?: ReactNode;
  subtitle?: string;
  showBreadcrumbs?: boolean;
  compact?: boolean;
}

const PageHeader = ({
  title,
  buttonText,
  buttonLink,
  buttonAction,
  startIcon,
  subtitle,
  showBreadcrumbs = true,
  compact = false
}: PageHeaderProps) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const location = useLocation();
  
  // Generate breadcrumbs from the current location
  const generateBreadcrumbs = () => {
    const paths = location.pathname.split('/').filter(path => path);
    
    // Create breadcrumb items
    const breadcrumbItems = [
      { name: 'Home', path: '/' }
    ];
    
    let currentPath = '';
    
    paths.forEach((path) => {
      currentPath += `/${path}`;
      // Format the path name to be more readable
      let name = path.charAt(0).toUpperCase() + path.slice(1);
      
      // Handle numeric IDs or special cases
      if (path === 'new') {
        name = 'New';
      } else if (!isNaN(Number(path)) || path.length > 20) {
        name = 'Details';
      }
      
      breadcrumbItems.push({
        name,
        path: currentPath
      });
    });
    
    return breadcrumbItems;
  };
  
  const breadcrumbs = generateBreadcrumbs();
  
  return (
    <Paper
      elevation={0}
      sx={{
        mb: compact ? 1.5 : 3,
        p: compact ? { xs: 1.5, md: 2 } : { xs: 2, md: 3 },
        borderRadius: 2,
        backgroundImage: `linear-gradient(to right, ${theme.palette.primary.main}15, ${theme.palette.background.paper})`,
        border: '1px solid',
        borderColor: 'divider',
      }}
    >
      <Box
        sx={{
          display: 'flex',
          flexDirection: isMobile ? 'column' : 'row',
          justifyContent: 'space-between',
          alignItems: isMobile ? 'flex-start' : 'center',
          mb: showBreadcrumbs ? (compact ? 1 : 2) : 0
        }}
      >
        <Box>
          <Typography
            variant={compact ? "h5" : "h4"}
            component="h1"
            sx={{
              fontWeight: 'bold',
              color: theme.palette.primary.main,
              mb: subtitle ? (compact ? 0.5 : 1) : 0,
              fontSize: compact ? '1.5rem' : undefined
            }}
          >
            {title}
          </Typography>

          {subtitle && (
            <Typography
              variant={compact ? "body2" : "subtitle1"}
              color="text.secondary"
              sx={{ mb: compact ? 0.5 : 1, fontSize: compact ? '0.875rem' : undefined }}
            >
              {subtitle}
            </Typography>
          )}
        </Box>
        
        {buttonText && (buttonLink || buttonAction) && (
          <Button
            variant="contained"
            color="primary"
            component={buttonLink ? RouterLink : 'button'}
            to={buttonLink}
            onClick={buttonAction}
            startIcon={startIcon}
            sx={{ 
              mt: isMobile ? 2 : 0,
              fontWeight: 'medium',
              px: 3,
              py: 1,
              borderRadius: 2,
              boxShadow: 2,
              '&:hover': {
                boxShadow: 4,
              }
            }}
          >
            {buttonText}
          </Button>
        )}
      </Box>
      
      {showBreadcrumbs && (
        <>
          <Divider sx={{ my: 1 }} />
          <Breadcrumbs 
            separator={<NavigateNextIcon fontSize="small" />} 
            aria-label="breadcrumb"
            sx={{ mt: 1 }}
          >
            {breadcrumbs.map((item, index) => {
              const isLast = index === breadcrumbs.length - 1;
              
              return isLast ? (
                <Typography key={item.path} color="text.primary" fontWeight="medium">
                  {item.name}
                </Typography>
              ) : (
                <Link
                  key={item.path}
                  component={RouterLink}
                  to={item.path}
                  underline="hover"
                  color="inherit"
                  sx={{ display: 'flex', alignItems: 'center' }}
                >
                  {index === 0 && <HomeIcon fontSize="small" sx={{ mr: 0.5 }} />}
                  {item.name}
                </Link>
              );
            })}
          </Breadcrumbs>
        </>
      )}
    </Paper>
  );
};

export default PageHeader;
