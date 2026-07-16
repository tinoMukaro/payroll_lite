package com.tino.payroll.lite.exception;

public class DuplicatePayrollRunException extends RuntimeException {
    public DuplicatePayrollRunException(String message) {
        super(message);
    }
}
