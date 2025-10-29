import { Chip, useTheme } from '@mui/material';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';
import CircleIcon from '@mui/icons-material/Circle';

type StatusType = 'success' | 'warning' | 'error' | 'info' | 'default';

interface StatusBadgeProps {
  status: string;
  statusMap?: Record<string, StatusType>;
  lowercase?: boolean;
  variant?: 'filled' | 'outlined';
  size?: 'small' | 'medium';
  withIcon?: boolean;
}

const StatusBadge = ({ 
  status, 
  statusMap,
  lowercase = false,
  variant = 'filled',
  size = 'small',
  withIcon = true
}: StatusBadgeProps) => {
  const theme = useTheme();
  
  // Default status mapping if not provided
  const defaultStatusMap: Record<string, StatusType> = {
    'ACTIVE': 'success',
    'OPEN': 'success',
    'INACTIVE': 'warning',
    'DEACTIVE': 'error',
    'CLOSED': 'error',
    'true': 'success',
    'false': 'error',
    'VERIFIED': 'success',
    'PENDING': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'error',
    'IN_PROGRESS': 'info',
    'PROCESSING': 'info',
    'DRAFT': 'default',
    'ENTRY': 'default'
  };

  const map = statusMap || defaultStatusMap;
  // Handle null or undefined status
  const statusKey = status ? status.toString().toUpperCase() : 'UNKNOWN';
  const colorType: StatusType = map[statusKey] || 'default';
  
  // Custom styling based on variant
  const getStyles = () => {
    // Safely get color from theme palette
    const getThemeColor = (type: StatusType) => {
      try {
        // Use default colors if theme palette doesn't have the color
        const defaultColors = {
          success: '#4caf50',
          warning: '#ff9800',
          error: '#f44336',
          info: '#2196f3',
          default: '#9e9e9e'
        };
        
        if (type === 'success' && theme.palette.success) {
          return theme.palette.success.main;
        } else if (type === 'error' && theme.palette.error) {
          return theme.palette.error.main;
        } else if (type === 'warning' && theme.palette.warning) {
          return theme.palette.warning.main;
        } else if (type === 'info' && theme.palette.info) {
          return theme.palette.info.main;
        }
        
        return defaultColors[type] || defaultColors.default;
      } catch {
        // Fallback colors if there's any error
        const fallbackColors = {
          success: '#4caf50',
          warning: '#ff9800',
          error: '#f44336',
          info: '#2196f3',
          default: '#9e9e9e'
        };
        return fallbackColors[type] || fallbackColors.default;
      }
    };

    if (variant === 'outlined') {
      return {
        border: `1px solid ${getThemeColor(colorType)}`,
        color: getThemeColor(colorType),
        backgroundColor: 'transparent'
      };
    }
    
    // Custom background colors for filled variant
    const bgColors = {
      success: `${getThemeColor('success')}15`,
      warning: `${getThemeColor('warning')}15`,
      error: `${getThemeColor('error')}15`,
      info: `${getThemeColor('info')}15`,
      default: `${getThemeColor('default')}15`
    };
    
    return {
      backgroundColor: bgColors[colorType] || bgColors.default,
      color: getThemeColor(colorType),
      border: `1px solid ${getThemeColor(colorType)}20`,
      fontWeight: 'medium'
    };
  };
  
  // Icons for each status type
  const icons = {
    success: <CheckCircleOutlineIcon fontSize="small" />,
    warning: <WarningAmberIcon fontSize="small" />,
    error: <ErrorOutlineIcon fontSize="small" />,
    info: <InfoOutlinedIcon fontSize="small" />,
    default: <CircleIcon fontSize="small" />
  };

  // Format display text
  const formatDisplayText = (text: string) => {
    // Handle null/undefined/empty text
    if (!text) return 'Unknown';
    
    if (lowercase) return text.toLowerCase();
    
    // Convert snake_case or CONSTANT_CASE to Title Case
    if (text.includes('_')) {
      return text
        .split('_')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
    }
    
    // Convert camelCase to Title Case
    if (/[a-z][A-Z]/.test(text)) {
      return text
        .replace(/([a-z])([A-Z])/g, '$1 $2')
        .replace(/\b\w/g, char => char.toUpperCase());
    }
    
    // If all uppercase, convert to Title Case
    if (text === text.toUpperCase()) {
      return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
    }
    
    return text;
  };

  const displayStatus = formatDisplayText(status ? status.toString() : 'Unknown');

  return (
    <Chip 
      label={displayStatus}
      size={size}
      icon={withIcon ? (icons[colorType] || icons.default) : undefined}
      sx={{
        ...getStyles(),
        borderRadius: '16px',
        '& .MuiChip-icon': {
          color: 'inherit'
        }
      }}
    />
  );
};

export default StatusBadge;
