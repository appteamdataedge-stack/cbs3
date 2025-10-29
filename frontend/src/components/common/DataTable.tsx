import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  CircularProgress,
  Typography,
  useTheme,
  useMediaQuery,
  Card,
  CardContent,
  Divider,
  Grid,
  Chip
} from '@mui/material';
import { useState } from 'react';
import type { ReactNode } from 'react';
import SortIcon from '@mui/icons-material/Sort';
import NoDataIllustration from '@mui/icons-material/InboxOutlined';

export interface Column<T> {
  id: keyof T | string;
  label: string;
  minWidth?: number;
  align?: 'left' | 'right' | 'center';
  format?: (value: any, row: T) => ReactNode;
  sortable?: boolean;
  hide?: 'sm' | 'md' | 'lg' | 'xl' | false;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  totalItems: number;
  page: number;
  rowsPerPage: number;
  onPageChange: (newPage: number) => void;
  onRowsPerPageChange: (newRowsPerPage: number) => void;
  onSort?: (sortField: string, sortDirection: 'asc' | 'desc') => void;
  idField?: keyof T;
  loading?: boolean;
  emptyContent?: ReactNode;
  title?: string;
}

const DataTable = <T extends object>({
  columns,
  rows,
  totalItems,
  page,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
  onSort,
  idField = 'id' as keyof T,
  loading = false,
  emptyContent,
  title
}: DataTableProps<T>) => {
  const [sortBy, setSortBy] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('asc');
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));
  const isTablet = useMediaQuery(theme.breakpoints.down('md'));

  const handleSort = (columnId: string) => {
    const isAsc = sortBy === columnId && sortDirection === 'asc';
    const newDirection = isAsc ? 'desc' : 'asc';
    
    setSortBy(columnId);
    setSortDirection(newDirection);
    
    if (onSort) {
      onSort(columnId, newDirection);
    }
  };

  const handleChangePage = (_event: unknown, newPage: number) => {
    onPageChange(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    onRowsPerPageChange(parseInt(event.target.value, 10));
    onPageChange(0);
  };

  // Filter columns based on screen size
  const visibleColumns = columns.filter(column => {
    if (!column.hide) return true;
    if (column.hide === 'sm' && isMobile) return false;
    if (column.hide === 'md' && isTablet) return false;
    return true;
  });

  // Render mobile card view
  const renderMobileView = () => {
    if (loading) {
      return (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
          <CircularProgress />
        </Box>
      );
    }

    if (rows.length === 0) {
      return renderEmptyState();
    }

    return (
      <Box>
        {rows.map((row, index) => (
          <Card 
            key={row[idField]?.toString() || index} 
            sx={{ 
              mb: 2, 
              border: '1px solid',
              borderColor: 'divider',
              '&:hover': {
                boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
              },
              transition: 'box-shadow 0.3s ease-in-out',
            }}
          >
            <CardContent>
              <Grid container spacing={1}>
                {columns.map((column) => {
                  const key = column.id.toString();
                  const value = key.includes('.') 
                    ? key.split('.').reduce((obj, i) => obj?.[i], row as any)
                    : (row as any)[key];
                  
                  return (
                    <Grid item xs={12} key={key}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 'bold' }}>
                          {column.label}:
                        </Typography>
                        <Box sx={{ textAlign: column.align || 'left' }}>
                          {column.format ? column.format(value, row) : value}
                        </Box>
                      </Box>
                      {column.id !== columns[columns.length - 1].id && <Divider sx={{ my: 1 }} />}
                    </Grid>
                  );
                })}
              </Grid>
            </CardContent>
          </Card>
        ))}
      </Box>
    );
  };

  // Render empty state
  const renderEmptyState = () => {
    if (emptyContent) {
      return emptyContent;
    }
    
    return (
      <Box sx={{ textAlign: 'center', py: 5 }}>
        <NoDataIllustration sx={{ fontSize: 60, color: 'text.secondary', opacity: 0.5 }} />
        <Typography variant="h6" color="text.secondary" sx={{ mt: 2 }}>
          No data available
        </Typography>
        <Typography variant="body2" color="text.secondary">
          There are no records to display
        </Typography>
      </Box>
    );
  };

  return (
    <Paper sx={{ width: '100%', overflow: 'hidden' }}>
      {title && (
        <Box sx={{ px: 3, py: 2, borderBottom: '1px solid', borderColor: 'divider' }}>
          <Typography variant="h6">{title}</Typography>
        </Box>
      )}
      
      {isMobile ? (
        // Mobile view
        <>
          {renderMobileView()}
        </>
      ) : (
        // Desktop view
        <TableContainer sx={{ maxHeight: 440 }}>
          <Table stickyHeader aria-label="data table">
            <TableHead>
              <TableRow>
                {visibleColumns.map((column) => (
                  <TableCell
                    key={column.id.toString()}
                    align={column.align || 'left'}
                    style={{ minWidth: column.minWidth }}
                    sx={{ 
                      fontWeight: 'bold',
                      whiteSpace: 'nowrap',
                      backgroundColor: theme.palette.background.default,
                    }}
                  >
                    {column.sortable && onSort ? (
                      <TableSortLabel
                        active={sortBy === column.id}
                        direction={sortBy === column.id ? sortDirection : 'asc'}
                        onClick={() => handleSort(column.id.toString())}
                        IconComponent={SortIcon}
                      >
                        {column.label}
                      </TableSortLabel>
                    ) : (
                      column.label
                    )}
                  </TableCell>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={visibleColumns.length} align="center" sx={{ py: 5 }}>
                    <CircularProgress size={40} />
                    <Typography variant="body2" sx={{ mt: 2 }}>
                      Loading data...
                    </Typography>
                  </TableCell>
                </TableRow>
              ) : rows.length > 0 ? (
                rows.map((row, index) => (
                  <TableRow 
                    hover 
                    tabIndex={-1} 
                    key={row[idField]?.toString() || index}
                    sx={{
                      '&:nth-of-type(odd)': {
                        backgroundColor: theme.palette.action.hover,
                      },
                      '&:hover': {
                        backgroundColor: `${theme.palette.primary.light}15`,
                      },
                    }}
                  >
                    {visibleColumns.map((column) => {
                      const key = column.id.toString();
                      const value = key.includes('.') 
                        ? key.split('.').reduce((obj, i) => obj?.[i], row as any)
                        : (row as any)[key];
                      
                      return (
                        <TableCell 
                          key={key} 
                          align={column.align}
                          sx={{
                            ...(column.id === 'actions' && {
                              padding: '4px 8px',
                              '& .MuiIconButton-root': {
                                padding: '4px',
                                margin: '0 2px',
                              },
                              '& .MuiBox-root': {
                                display: 'flex',
                                alignItems: 'center',
                                gap: '4px',
                              }
                            })
                          }}
                        >
                          {column.format ? column.format(value, row) : value}
                        </TableCell>
                      );
                    })}
                  </TableRow>
                ))
              ) : (
                <TableRow>
                  <TableCell colSpan={visibleColumns.length}>
                    {renderEmptyState()}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      
      <Box sx={{ 
        display: 'flex', 
        justifyContent: 'space-between',
        alignItems: 'center',
        flexDirection: isMobile ? 'column' : 'row',
        px: 2,
        py: 1,
        borderTop: '1px solid',
        borderColor: 'divider'
      }}>
        <Box sx={{ mb: isMobile ? 1 : 0 }}>
          <Chip 
            label={`Total: ${totalItems} records`} 
            size="small" 
            sx={{ bgcolor: theme.palette.primary.main + '20', color: 'primary.main' }}
          />
        </Box>
        <TablePagination
          rowsPerPageOptions={[5, 10, 25, 50]}
          component="div"
          count={totalItems}
          rowsPerPage={rowsPerPage}
          page={page}
          onPageChange={handleChangePage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          labelRowsPerPage={isMobile ? '' : 'Rows:'}
          labelDisplayedRows={({ from, to, count }) => `${from}-${to} of ${count}`}
          sx={{
            '.MuiTablePagination-selectLabel': {
              margin: 0,
            },
            '.MuiTablePagination-displayedRows': {
              margin: 0,
            },
          }}
        />
      </Box>
    </Paper>
  );
};

export default DataTable;
