package com.ecommerce.payment_service.exception;

public class DownstreamServiceUnavailableException
        extends RuntimeException {

    public DownstreamServiceUnavailableException(String message) {
        super(message);
    }
}
