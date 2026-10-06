package com.fooddelivery.order.exception;

public class FoodNotFoundException extends RuntimeException {

    public FoodNotFoundException(String message) {
        super(message);
    }
}
