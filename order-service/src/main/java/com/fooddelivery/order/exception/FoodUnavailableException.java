package com.fooddelivery.order.exception;

public class FoodUnavailableException extends RuntimeException {

    public FoodUnavailableException(String message) {
        super(message);
    }
}
