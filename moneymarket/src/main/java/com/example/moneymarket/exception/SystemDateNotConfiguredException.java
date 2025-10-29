package com.example.moneymarket.exception;

/**
 * Exception thrown when System_Date is not configured in Parameter_Table
 * This ensures CBS compliance by preventing device clock usage
 */
public class SystemDateNotConfiguredException extends RuntimeException {
    
    public SystemDateNotConfiguredException(String message) {
        super(message);
    }
    
    public SystemDateNotConfiguredException(String message, Throwable cause) {
        super(message, cause);
    }
}
