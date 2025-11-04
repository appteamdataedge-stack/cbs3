import { useState } from 'react';
import { AppBar, Box, Button, Container, Toolbar, Typography, IconButton, Menu, MenuItem, Drawer, List, ListItem, ListItemText, useTheme, useMediaQuery, Divider } from '@mui/material';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import MenuIcon from '@mui/icons-material/Menu';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import CategoryIcon from '@mui/icons-material/Category';
import ViewListIcon from '@mui/icons-material/ViewList';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import BusinessIcon from '@mui/icons-material/Business';
import PaymentIcon from '@mui/icons-material/Payment';
import DescriptionIcon from '@mui/icons-material/Description';
import SettingsIcon from '@mui/icons-material/Settings';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import AccountCircle from '@mui/icons-material/AccountCircle';
import LogoutIcon from '@mui/icons-material/Logout';
import { useAuth } from '../../context/AuthContext';

const Navbar = () => {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const location = useLocation();
  const { logout } = useAuth();
  
  const navItems = [
    { name: 'Dashboard', path: '/', icon: <DashboardIcon /> },
    { name: 'Customers', path: '/customers', icon: <PeopleIcon /> },
    { name: 'Products', path: '/products', icon: <CategoryIcon /> },
    { name: 'SubProducts', path: '/subproducts', icon: <ViewListIcon /> },
    { name: 'Accounts', path: '/accounts', icon: <AccountBalanceIcon /> },
    { name: 'Office Accounts', path: '/office-accounts', icon: <BusinessIcon /> },
    { name: 'Transactions', path: '/transactions', icon: <PaymentIcon /> },
    { name: 'Statement of Accounts', path: '/statement-of-accounts', icon: <DescriptionIcon /> },
    { name: 'System Date', path: '/admin/system-date', icon: <CalendarTodayIcon /> },
    { name: 'EOD', path: '/admin/eod', icon: <SettingsIcon /> },
  ];


  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleDrawerToggle = () => {
    setMobileMenuOpen(!mobileMenuOpen);
  };

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === path;
    }
    return location.pathname.startsWith(path);
  };

  const drawer = (
    <Box onClick={handleDrawerToggle} sx={{ textAlign: 'center' }}>
      <Typography variant="h6" sx={{ my: 2, fontWeight: 700 }}>
        Money Market
      </Typography>
      <List>
        {navItems.map((item) => (
          <ListItem 
            key={item.name} 
            component={RouterLink} 
            to={item.path}
            sx={{ 
              color: isActive(item.path) ? 'primary.main' : 'text.primary',
              bgcolor: isActive(item.path) ? 'action.selected' : 'transparent',
              textDecoration: 'none'
            }}
          >
            <Box sx={{ mr: 2 }}>{item.icon}</Box>
            <ListItemText primary={item.name} />
          </ListItem>
        ))}
      </List>
    </Box>
  );

  return (
    <AppBar position="static" elevation={3} sx={{ backgroundColor: 'white', color: 'primary.main' }}>
      <Container maxWidth="xl">
        <Toolbar disableGutters>
          {isMobile && (
            <IconButton
              color="inherit"
              aria-label="open drawer"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ mr: 2 }}
            >
              <MenuIcon />
            </IconButton>
          )}
          <Typography
            variant="h6"
            noWrap
            component={RouterLink}
            to="/"
            sx={{
              mr: 2,
              display: 'flex',
              fontWeight: 700,
              color: 'inherit',
              textDecoration: 'none',
            }}
          >
            Money Market
          </Typography>
          
          {!isMobile && (
            <Box sx={{ flexGrow: 1, display: 'flex' }}>
              {navItems.map((item) => (
                <Button
                  key={item.name}
                  component={RouterLink}
                  to={item.path}
                  startIcon={item.icon}
                  sx={{ 
                    mx: 0.5,
                    color: isActive(item.path) ? 'primary.dark' : 'primary.main',
                    backgroundColor: isActive(item.path) ? 'rgba(25, 118, 210, 0.08)' : 'transparent',
                    '&:hover': {
                      backgroundColor: 'rgba(25, 118, 210, 0.12)',
                    },
                    fontWeight: isActive(item.path) ? 'bold' : 'normal',
                  }}
                >
                  {item.name}
                </Button>
              ))}
            </Box>
          )}
          
          <Box sx={{ flexGrow: 0, display: 'flex', alignItems: 'center' }}>
            <Box sx={{ 
              display: { xs: 'none', md: 'flex' }, 
              alignItems: 'center', 
              mr: 2,
              borderRight: '1px solid',
              borderColor: 'divider',
              pr: 2
            }}>
              <Typography variant="subtitle2" color="inherit">
                {new Date().toLocaleDateString()}
              </Typography>
            </Box>
            
            <IconButton
              onClick={handleMenuOpen}
              sx={{ 
                p: 1,
                borderRadius: 1,
                border: '1px solid',
                borderColor: 'divider',
                backgroundColor: 'rgba(255,255,255,0.1)'
              }}
              color="inherit"
              aria-label="user menu"
            >
              <SettingsIcon />
            </IconButton>
            <Menu
              sx={{ mt: '45px' }}
              id="menu-appbar"
              anchorEl={anchorEl}
              anchorOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
              keepMounted
              transformOrigin={{
                vertical: 'top',
                horizontal: 'right',
              }}
              open={Boolean(anchorEl)}
              onClose={handleMenuClose}
              PaperProps={{
                elevation: 3,
                sx: {
                  minWidth: 200,
                  borderRadius: 2,
                  overflow: 'visible',
                  mt: 1.5,
                  '&:before': {
                    content: '""',
                    display: 'block',
                    position: 'absolute',
                    top: 0,
                    right: 14,
                    width: 10,
                    height: 10,
                    bgcolor: 'background.paper',
                    transform: 'translateY(-50%) rotate(45deg)',
                    zIndex: 0,
                  },
                },
              }}
            >
              <Box sx={{ px: 2, py: 1.5 }}>
                <Typography variant="subtitle1" fontWeight="bold">Admin User</Typography>
                <Typography variant="body2" color="text.secondary">admin@example.com</Typography>
              </Box>
              <Divider />
              <MenuItem onClick={handleMenuClose} sx={{ py: 1.5 }}>
                <Box component="span" sx={{ pr: 2, display: 'inline-flex' }}>
                  <AccountCircle fontSize="small" />
                </Box>
                Profile
              </MenuItem>
              <MenuItem onClick={handleMenuClose} sx={{ py: 1.5 }}>
                <Box component="span" sx={{ pr: 2, display: 'inline-flex' }}>
                  <SettingsIcon fontSize="small" />
                </Box>
                Settings
              </MenuItem>
              <Divider />
              <MenuItem onClick={() => {
                handleMenuClose();
                logout(); // Use the imported logout function
              }} sx={{ py: 1.5, color: 'error.main' }}>
                <Box component="span" sx={{ pr: 2, display: 'inline-flex' }}>
                  <LogoutIcon fontSize="small" />
                </Box>
                Logout
              </MenuItem>
            </Menu>
          </Box>
        </Toolbar>
      </Container>
      
      <Drawer
        variant="temporary"
        open={mobileMenuOpen}
        onClose={handleDrawerToggle}
        ModalProps={{
          keepMounted: true, // Better mobile performance
        }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': { boxSizing: 'border-box', width: 240 },
        }}
      >
        {drawer}
      </Drawer>
    </AppBar>
  );
};

export default Navbar;
