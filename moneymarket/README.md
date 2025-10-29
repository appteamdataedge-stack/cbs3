# Money Market Module

This project is a production-ready, scalable Spring Boot application implementing a Money Market Module for a Core Banking System.

## Technology Stack

- Java 17
- Spring Boot 3.1.5
- Spring Data JPA
- Spring Validation
- Spring Actuator
- Spring Retry
- Lombok
- Flyway for database migrations
- MySQL 8.0
- OpenAPI/Swagger (springdoc) for API documentation
- JUnit 5 & Mockito for testing
- Docker & Docker Compose for containerization

## Requirements

- Java 17 or higher
- Maven
- Docker (for containerized deployment)
- MySQL 8.0 (if running locally)

## Building and Running the Application

### Using Maven

1. Clone the repository
2. Navigate to the project root directory
3. Build the project:
   ```bash
   mvn clean package
   ```
4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

### Using Docker Compose

Docker Compose will automatically set up both the MySQL database and the application:

```bash
docker-compose up --build
```

The application will be available at http://localhost:8080

### Environment Variables

The following environment variables can be configured:

- `SPRING_DATASOURCE_URL`: JDBC URL for the database (default: jdbc:mysql://127.0.0.1:3306/moneymarketdb)
- `SPRING_DATASOURCE_USERNAME`: Database username (default: root)
- `SPRING_DATASOURCE_PASSWORD`: Database password (default: yourpassword)
- `SPRING_PROFILES_ACTIVE`: Active Spring profile (default: dev)

## Database Setup

The application uses Flyway to manage database migrations. On startup, it automatically:

1. Creates the necessary tables (if they don't exist)
2. Inserts sample data for testing

## API Documentation

The API documentation is available via Swagger UI at:
- http://localhost:8080/swagger-ui.html

## Main Functionalities

### 1. Customer Management
- Create, update, and view customers
- Support for Individual, Corporate, and Bank customer types
- Maker-checker verification flow

### 2. Product & Sub-Product Management
- Create and manage product hierarchy
- Sub-product lifecycle management (Active, Inactive, Deactive)
- GL account association and validation

### 3. Account Management
- Customer accounts and office accounts
- Account opening and closure
- Balance tracking

### 4. Transaction Processing
- Multi-leg balanced transactions
- GL movement tracking
- Balance updates

### 5. Interest Accruals
- End-of-day (EOD) processing
- Interest calculation and posting
- Manual and scheduled EOD runs

## API Examples

### Create a Customer

```bash
curl -X POST http://localhost:8080/api/customers \
  -H "Content-Type: application/json" \
  -d '{
    "extCustId": "CUST123",
    "custType": "Individual",
    "firstName": "John",
    "lastName": "Doe",
    "address1": "123 Main St",
    "mobile": "1234567890",
    "makerId": "ADMIN"
  }'
```

### Create a Product

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{
    "productCode": "PROD001",
    "productName": "Money Market Loan",
    "cumGLNum": "1101",
    "makerId": "ADMIN"
  }'
```

### Create a Transaction

```bash
curl -X POST http://localhost:8080/api/transactions/entry \
  -H "Content-Type: application/json" \
  -d '{
    "valueDate": "2023-10-30",
    "narration": "Loan Disbursement",
    "lines": [
      {
        "accountNo": "110101001001",
        "drCrFlag": "D",
        "tranCcy": "USD",
        "fcyAmt": 10000.00,
        "exchangeRate": 1.0000,
        "lcyAmt": 10000.00
      },
      {
        "accountNo": "210101001001",
        "drCrFlag": "C",
        "tranCcy": "USD",
        "fcyAmt": 10000.00,
        "exchangeRate": 1.0000,
        "lcyAmt": 10000.00
      }
    ]
  }'
```

### Run EOD Process Manually

```bash
curl -X POST "http://localhost:8080/api/admin/run-eod"
```

## Error Handling

The application returns structured JSON responses for errors with appropriate HTTP status codes:

- 400 Bad Request: Validation errors or business rule violations
- 404 Not Found: Requested resource not found
- 500 Internal Server Error: Unexpected errors

Example error response:

```json
{
  "timestamp": "2023-10-30T12:34:56",
  "status": 400,
  "error": "Business Rule Violation",
  "message": "Debit amount does not equal credit amount. Please correct the entries.",
  "details": ["Transaction is not balanced"],
  "path": "/api/transactions/entry"
}
```

## Design Decisions & Implementation Notes

1. **Account Number Generation**: Follows the SRS algorithm (GL_Num + 3-digit sequential counter) with pessimistic locking to ensure concurrency safety.

2. **GL Number Generation**: Implements the concatenation of Layer_GL_Num values as specified in the SRS.

3. **Transaction Processing**: Implements balanced multi-leg transactions with GL movements and balance updates in a single transaction.

4. **Interest Accrual**: Automated EOD process with manual trigger option.

5. **Validation**: Implements all SRS validation rules with precise error messages.

## Assumptions

1. The database is MySQL and supports pessimistic locking for critical operations.
2. Interest rates are configured per sub-product via the intt_code.
3. For demo purposes, GL hierarchy is pre-configured (Layer 0-4).
4. Transaction balancing is enforced at the application level.

## Future Enhancements

1. Full security implementation with Spring Security
2. Complete Maker-Checker workflow with role-based access
3. More sophisticated interest calculation methods
4. Additional reporting capabilities
5. Integration with external systems
