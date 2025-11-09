import { useState } from 'react';
import { 
  Box, 
  Drawer, 
  List, 
  ListItem, 
  ListItemButton, 
  ListItemIcon, 
  ListItemText, 
  Typography, 
  IconButton, 
  Menu, 
  MenuItem, 
  Divider,
  useTheme, 
  useMediaQuery,
  Avatar,
  Tooltip
} from '@mui/material';
import { Link as RouterLink, useLocation } from 'react-router-dom';
import DashboardIcon from '@mui/icons-material/Dashboard';
import PeopleIcon from '@mui/icons-material/People';
import CategoryIcon from '@mui/icons-material/Category';
import ViewListIcon from '@mui/icons-material/ViewList';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import BusinessIcon from '@mui/icons-material/Business';
import PaymentIcon from '@mui/icons-material/Payment';
import SettingsIcon from '@mui/icons-material/Settings';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import DescriptionIcon from '@mui/icons-material/Description';
import AccountCircle from '@mui/icons-material/AccountCircle';
import LogoutIcon from '@mui/icons-material/Logout';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import { useAuth } from '../../context/AuthContext';

const drawerWidth = 280;
const collapsedDrawerWidth = 64;

interface SidebarProps {
  open: boolean;
  onToggle: () => void;
}

const Sidebar = ({ open, onToggle }: SidebarProps) => {
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
    { name: 'BOD', path: '/admin/bod', icon: <SettingsIcon /> },
    { name: 'EOD', path: '/admin/eod', icon: <SettingsIcon /> },
  ];

  const handleMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    logout();
    handleMenuClose();
  };

  const isActive = (path: string) => {
    return location.pathname === path;
  };

  const sidebarContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box sx={{ 
        p: 2, 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: open ? 'space-between' : 'center',
        borderBottom: '1px solid',
        borderColor: 'divider',
        minHeight: 64
      }}>
        {open && (
          <Typography 
            variant="h6" 
            component={RouterLink}
            to="/"
            sx={{ 
              fontWeight: 700, 
              color: 'primary.main',
              textDecoration: 'none',
              flexGrow: 1
            }}
          >
            Money Market
          </Typography>
        )}
        <IconButton 
          onClick={onToggle}
          sx={{ 
            color: 'primary.main',
            '&:hover': {
              backgroundColor: 'primary.50'
            }
          }}
        >
          {open ? <ChevronLeftIcon /> : <ChevronRightIcon />}
        </IconButton>
      </Box>

      {/* Navigation Items */}
      <Box sx={{ flexGrow: 1, overflow: 'auto' }}>
        <List sx={{ px: 1, py: 2 }}>
          {navItems.map((item) => (
            <ListItem key={item.name} disablePadding sx={{ mb: 0.5 }}>
              <Tooltip title={!open ? item.name : ''} placement="right">
                <ListItemButton
                  component={RouterLink}
                  to={item.path}
                  sx={{
                    borderRadius: 2,
                    minHeight: 48,
                    px: 2,
                    color: isActive(item.path) ? 'primary.main' : 'text.primary',
                    backgroundColor: isActive(item.path) ? 'primary.50' : 'transparent',
                    borderLeft: isActive(item.path) ? '4px solid' : '4px solid transparent',
                    borderLeftColor: isActive(item.path) ? 'primary.main' : 'transparent',
                    '&:hover': {
                      backgroundColor: isActive(item.path) ? 'primary.100' : 'action.hover',
                      color: 'primary.main',
                    },
                    transition: 'all 0.2s ease-in-out',
                  }}
                >
                  <ListItemIcon 
                    sx={{ 
                      minWidth: open ? 40 : 'auto',
                      color: isActive(item.path) ? 'primary.main' : 'inherit',
                      justifyContent: 'center'
                    }}
                  >
                    {item.icon}
                  </ListItemIcon>
                  {open && (
                    <ListItemText 
                      primary={item.name}
                      primaryTypographyProps={{
                        fontWeight: isActive(item.path) ? 600 : 400,
                        fontSize: '0.875rem'
                      }}
                    />
                  )}
                </ListItemButton>
              </Tooltip>
            </ListItem>
          ))}
        </List>
      </Box>

      {/* Footer - User Menu */}
      <Box sx={{ 
        p: 2, 
        borderTop: '1px solid',
        borderColor: 'divider'
      }}>
        {open ? (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Avatar sx={{ bgcolor: 'primary.main', width: 32, height: 32 }}>
              <AccountCircle />
            </Avatar>
            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
              <Typography variant="subtitle2" noWrap>
                Admin User
              </Typography>
              <Typography variant="caption" color="text.secondary" noWrap>
                {new Date().toLocaleDateString()}
              </Typography>
            </Box>
            <IconButton
              onClick={handleMenuOpen}
              size="small"
              sx={{ 
                color: 'text.secondary',
                '&:hover': {
                  backgroundColor: 'action.hover'
                }
              }}
            >
              <SettingsIcon fontSize="small" />
            </IconButton>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', justifyContent: 'center' }}>
            <Tooltip title="User Menu" placement="right">
              <IconButton
                onClick={handleMenuOpen}
                sx={{ 
                  color: 'text.secondary',
                  '&:hover': {
                    backgroundColor: 'action.hover'
                  }
                }}
              >
                <AccountCircle />
              </IconButton>
            </Tooltip>
          </Box>
        )}
      </Box>

      {/* User Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleMenuClose}
        transformOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'top' }}
        PaperProps={{
          sx: {
            mt: 1,
            minWidth: 200,
            boxShadow: '0px 4px 20px rgba(0,0,0,0.1)',
            borderRadius: 2,
          }
        }}
      >
        <MenuItem onClick={handleMenuClose}>
          <ListItemIcon>
            <AccountCircle fontSize="small" />
          </ListItemIcon>
          Profile
        </MenuItem>
        <MenuItem onClick={handleMenuClose}>
          <ListItemIcon>
            <SettingsIcon fontSize="small" />
          </ListItemIcon>
          Settings
        </MenuItem>
        <Divider />
        <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
          <ListItemIcon>
            <LogoutIcon fontSize="small" sx={{ color: 'error.main' }} />
          </ListItemIcon>
          Logout
        </MenuItem>
      </Menu>
    </Box>
  );

  return (
    <>
      {/* Mobile Drawer */}
      <Drawer
        variant="temporary"
        open={open && isMobile}
        onClose={onToggle}
        ModalProps={{
          keepMounted: true, // Better open performance on mobile.
        }}
        sx={{
          display: { xs: 'block', md: 'none' },
          '& .MuiDrawer-paper': {
            boxSizing: 'border-box',
            width: drawerWidth,
            borderRight: 'none',
            boxShadow: '0px 0px 20px rgba(0,0,0,0.1)',
          },
        }}
      >
        {sidebarContent}
      </Drawer>

      {/* Desktop Drawer */}
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', md: 'block' },
          '& .MuiDrawer-paper': {
            boxSizing: 'border-box',
            width: open ? drawerWidth : collapsedDrawerWidth,
            borderRight: '1px solid',
            borderColor: 'divider',
            transition: theme.transitions.create('width', {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.enteringScreen,
            }),
            overflowX: 'hidden',
            backgroundColor: 'background.paper',
          },
        }}
        open={open}
      >
        {sidebarContent}
      </Drawer>
    </>
  );
};

export default Sidebar;
