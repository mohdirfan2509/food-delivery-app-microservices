package com.fooddelivery.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderItemRequest {

    @NotNull(message = "Food ID is required")
    @Positive(message = "Food ID must be positive")
    private Long foodId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    @Max(value = 100, message = "Quantity must not exceed 100")
    private Integer quantity;

    public OrderItemRequest() {
    }

    public OrderItemRequest(Long foodId, Integer quantity) {
        this.foodId = foodId;
        this.quantity = quantity;
    }

    public Long getFoodId() {
        return foodId;
    }

    public void setFoodId(Long foodId) {
        this.foodId = foodId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
