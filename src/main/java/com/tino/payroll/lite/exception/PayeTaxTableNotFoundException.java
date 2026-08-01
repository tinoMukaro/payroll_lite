package com.tino.payroll.lite.exception;

public class PayeTaxTableNotFoundException extends RuntimeException {
    public PayeTaxTableNotFoundException(String message) {
        super(message);
    }
}
