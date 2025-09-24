package com.smu.tariff.exception;

public class InvalidTariffRequestException extends RuntimeException {
    
    public InvalidTariffRequestException(String message) {
        super(message);
    }
    
    public InvalidTariffRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
