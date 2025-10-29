# Money Market Frontend

This project is a React frontend for the Money Market banking system. It provides a user interface for managing customers, products, accounts, transactions, and other core banking functions.

## Technology Stack

- **React 18** with **TypeScript**
- **Vite** for build tooling
- **React Router v6** for routing
- **Material UI** for UI components
- **TanStack Query** (React Query) for API data fetching
- **Axios** for HTTP requests
- **React Hook Form** for form handling
- **React Toastify** for notifications

## Prerequisites

- Node.js (v16 or newer)
- NPM (v7 or newer)
- Money Market backend running on http://localhost:8080/api (or configured URL)

## Getting Started

### Installation

1. Clone the repository
2. Navigate to the frontend directory:
   ```
   cd frontend
   ```
3. Install dependencies:
   ```
   npm install
   ```
4. Create a `.env` file in the root directory with the following content:
   ```
   VITE_API_BASE_URL=http://localhost:8080/api
   ```
   (Adjust the URL if your backend runs on a different host/port)

### Development

To start the development server:

```
npm run dev
```

This will start the development server at http://localhost:5173 (default Vite port).

### Building for Production

To build the application for production:

```
npm run build
```

The build output will be in the `dist` directory, which you can deploy to any static hosting service.

To preview the production build locally:

```
npm run preview
```

## Application Structure

- `src/api/` - API service functions
- `src/components/` - Reusable UI components
- `src/pages/` - Application pages/views
- `src/routes/` - Routing configuration
- `src/types/` - TypeScript type definitions
- `src/utils/` - Utility functions

## Features

1. **Dashboard**
   - Overview of key metrics
   - Quick links to common tasks

2. **Customer Management**
   - Create, view, and edit customers
   - Verify customers (maker-checker workflow)

3. **Product Management**
   - Manage products and sub-products
   - Interest rate and GL account configurations

4. **Account Management**
   - Open new customer accounts
   - View account details
   - Close accounts

5. **Transaction Processing**
   - Create multi-leg transactions
   - View transaction history

6. **End of Day Processing**
   - Run interest accruals
   - View EOD process results

## Authentication

The current implementation does not include authentication. The application is designed with placeholders for user identification (maker/checker IDs), which can be replaced with actual user authentication in a future phase.

## Backend Integration

The frontend is designed to work with the Money Market Spring Boot backend. All API calls are configured to use the base URL specified in the `.env` file.

## Known Issues and Limitations

- Transaction history view currently shows mock data, as the backend API for listing transactions is not implemented yet.
- Some validation rules may need adjustment based on specific business requirements.
- The application assumes the backend is running and accessible; appropriate error handling is in place, but a full offline mode is not implemented.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -am 'Add your feature'`)
4. Push to the branch (`git push origin feature/your-feature`)
5. Create a new Pull Request