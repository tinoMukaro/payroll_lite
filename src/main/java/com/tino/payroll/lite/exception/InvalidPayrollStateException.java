package com.tino.payroll.lite.exception;

public class InvalidPayrollStateException extends RuntimeException {
    public InvalidPayrollStateException(String message) {
        super(message);
    }
}
