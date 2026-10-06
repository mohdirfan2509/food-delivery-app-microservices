package com.fooddelivery.food.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class FoodItemResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long foodId;
    private Long restaurantId;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private Boolean available;

    public FoodItemResponse() {
    }

    public FoodItemResponse(Long foodId, Long restaurantId, String name, String description, BigDecimal price, String category, Boolean available) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FoodItemResponse that = (FoodItemResponse) o;
        return Objects.equals(foodId, that.foodId) &&
                Objects.equals(restaurantId, that.restaurantId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(price, that.price) &&
                Objects.equals(category, that.category) &&
                Objects.equals(available, that.available);
    }

    @Override
    public int hashCode() {
        return Objects.hash(foodId, restaurantId, name, description, price, category, available);
    }

    @Override
    public String toString() {
        return "FoodItemResponse{" +
                "foodId=" + foodId +
                ", restaurantId=" + restaurantId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", price=" + price +
                ", category='" + category + '\'' +
                ", available=" + available +
                '}';
    }
}
