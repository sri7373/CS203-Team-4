package com.smu.tariff.exception;

public class TariffNotFoundException extends RuntimeException {
    
    public TariffNotFoundException(String message) {
        super(message);
    }
    
    public TariffNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
