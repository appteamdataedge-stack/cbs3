import { Box, CssBaseline, ThemeProvider, Typography, createTheme, responsiveFontSizes, AppBar, Toolbar, IconButton, useTheme, useMediaQuery } from '@mui/material';
import type { ReactNode } from 'react';
import { useState } from 'react';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';
import Sidebar from './Sidebar';
import MenuIcon from '@mui/icons-material/Menu';

// Create a theme instance
let theme = createTheme({
  palette: {
    primary: {
      light: '#4dabf5',
      main: '#1976d2',
      dark: '#1565c0',
      contrastText: '#fff',
    },
    secondary: {
      light: '#ff4081',
      main: '#f50057',
      dark: '#c51162',
      contrastText: '#fff',
    },
    background: {
      default: '#f5f5f5',
      paper: '#ffffff',
    },
    text: {
      primary: '#333333',
      secondary: '#666666',
    },
    success: {
      main: '#4caf50',
    },
    error: {
      main: '#f44336',
    },
    warning: {
      main: '#ff9800',
    },
    info: {
      main: '#2196f3',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h1: {
      fontWeight: 500,
    },
    h2: {
      fontWeight: 500,
    },
    h3: {
      fontWeight: 500,
    },
    h4: {
      fontWeight: 500,
    },
    h5: {
      fontWeight: 500,
    },
    h6: {
      fontWeight: 500,
    },
    button: {
      textTransform: 'none',
      fontWeight: 500,
    },
  },
  shape: {
    borderRadius: 8,
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: '8px 16px',
          boxShadow: 'none',
          '&:hover': {
            boxShadow: '0px 2px 4px rgba(0, 0, 0, 0.1)',
          },
        },
        contained: {
          boxShadow: '0px 1px 2px rgba(0, 0, 0, 0.05)',
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          boxShadow: '0px 2px 8px rgba(0, 0, 0, 0.08)',
        },
        elevation1: {
          boxShadow: '0px 1px 3px rgba(0, 0, 0, 0.12)',
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          overflow: 'hidden',
        },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          padding: '8px 12px',
        },
        head: {
          fontWeight: 600,
          backgroundColor: 'rgba(25, 118, 210, 0.04)',
          padding: '12px',
        },
      },
    },
  },
});

// Make fonts responsive
theme = responsiveFontSizes(theme);

interface LayoutProps {
  children: ReactNode;
}

const Layout = ({ children }: LayoutProps) => {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  // Sidebar width constants (must match Sidebar.tsx)
  const drawerWidth = 280;
  const collapsedDrawerWidth = 64;

  const handleSidebarToggle = () => {
    setSidebarOpen(!sidebarOpen);
  };


  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
        {/* Sidebar */}
        <Sidebar open={sidebarOpen} onToggle={handleSidebarToggle} />

        {/* Main Content Area */}
        <Box 
          component="main" 
          sx={{ 
            display: 'flex',
            flexDirection: 'column',
            minWidth: 0, // Prevent flex item from overflowing
            overflow: 'hidden', // Ensure content doesn't overflow
            // CRITICAL FIX: Offset content past the fixed sidebar
            marginLeft: { md: sidebarOpen ? `${drawerWidth}px` : `${collapsedDrawerWidth}px` },
            width: { md: sidebarOpen ? `calc(100vw - ${drawerWidth}px)` : `calc(100vw - ${collapsedDrawerWidth}px)` },
            transition: theme.transitions.create(['margin-left', 'width'], {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.enteringScreen,
            })
          }}
        >
          {/* Mobile App Bar */}
          {isMobile && (
            <AppBar 
              position="fixed" 
              sx={{ 
                display: { xs: 'block', md: 'none' },
                bgcolor: 'background.paper',
                color: 'text.primary',
                boxShadow: '0px 1px 3px rgba(0,0,0,0.1)',
                zIndex: theme.zIndex.drawer + 1,
                width: '100vw', // Full viewport width on mobile
                left: 0 // Ensure it starts from the left edge
              }}
            >
              <Toolbar>
                <IconButton
                  color="inherit"
                  aria-label="open drawer"
                  edge="start"
                  onClick={handleSidebarToggle}
                  sx={{ mr: 2 }}
                >
                  <MenuIcon />
                </IconButton>
                <Typography variant="h6" noWrap component="div" sx={{ fontWeight: 700, color: 'primary.main' }}>
                  Money Market
                </Typography>
              </Toolbar>
            </AppBar>
          )}

          {/* Content */}
          <Box 
            sx={{ 
              flexGrow: 1,
              pt: { xs: 8, md: 3 },
              pb: 3,
              px: { xs: 1, sm: 2, md: 3 },
              width: '100%',
              display: 'flex',
              flexDirection: 'column',
              minWidth: 0 // Ensure flex item can shrink
            }}
          >
            <Box
              sx={{
                borderRadius: 2,
                overflow: 'hidden',
                boxShadow: '0 4px 20px rgba(0,0,0,0.05)',
                bgcolor: 'background.paper',
                p: { xs: 2, sm: 3, md: 4 },
                width: '100%',
                maxWidth: 'none',
                minHeight: 'calc(100vh - 120px)',
                flexGrow: 1,
                minWidth: 0 // Allow content to use full width
              }}
            >
              {children}
            </Box>
          </Box>

          {/* Footer */}
          <Box 
            component="footer" 
            sx={{ 
              py: 3, 
              textAlign: 'center', 
              borderTop: '1px solid',
              borderColor: 'divider',
              bgcolor: 'background.paper',
              mt: 'auto',
              width: '100%'
            }}
          >
            <Box sx={{ 
              width: '100%',
              px: { xs: 2, sm: 3, md: 4 },
              mx: 'auto'
            }}>
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexDirection: { xs: 'column', sm: 'row' } }}>
                <Typography variant="body2" color="text.secondary">
                  Developed By dataedge limited &copy; {new Date().getFullYear()}
                </Typography>
                <Box sx={{ mt: { xs: 1, sm: 0 } }}>
                  <Typography variant="body2" color="text.secondary" component="span" sx={{ mx: 1 }}>
                    Privacy Policy
                  </Typography>
                  <Typography variant="body2" color="text.secondary" component="span" sx={{ mx: 1 }}>
                    Terms of Service
                  </Typography>
                  <Typography variant="body2" color="text.secondary" component="span" sx={{ mx: 1 }}>
                    Contact
                  </Typography>
                </Box>
              </Box>
            </Box>
          </Box>
        </Box>
      </Box>
      <ToastContainer 
        position="top-right" 
        autoClose={3000} 
        hideProgressBar={false}
        newestOnTop
        closeOnClick
        rtl={false}
        pauseOnFocusLoss
        draggable
        pauseOnHover
        theme="colored"
      />
    </ThemeProvider>
  );
};

export default Layout;
