package com.tino.payroll.lite.exception;

public class PayrollRunNotFoundException extends RuntimeException {
    public PayrollRunNotFoundException(String message) {
        super(message);
    }
}
