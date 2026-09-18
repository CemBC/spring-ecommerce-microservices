package com.ecommerce.order_service.exception;

public class DownstreamServiceUnavailableException
        extends RuntimeException {

    public DownstreamServiceUnavailableException(String message) {
        super(message);
    }
}
