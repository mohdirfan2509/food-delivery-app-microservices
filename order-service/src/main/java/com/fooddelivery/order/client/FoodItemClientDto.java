package com.fooddelivery.order.client;

import java.math.BigDecimal;

public class FoodItemClientDto {

    private Long foodId;
    private Long restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Boolean available;

    public FoodItemClientDto() {
    }

    public FoodItemClientDto(Long foodId, Long restaurantId, String name, BigDecimal price, Boolean available) {
        this.foodId = foodId;
        this.restaurantId = restaurantId;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public FoodItemClientDto(Long foodId, Long restaurantId, String name, String description,
                             BigDecimal price, String category, Boolean available) {
        this.foodId = foodId;
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.available = available;
    }

    public Long getFoodId() {
        return foodId;
    }

    public void setFoodId(Long foodId) {
        this.foodId = foodId;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
