package com.fooddelivery.food.service;

import com.fooddelivery.food.dto.FoodItemCreateRequest;
import com.fooddelivery.food.dto.FoodItemResponse;
import com.fooddelivery.food.dto.FoodItemUpdateRequest;

import java.util.List;

public interface FoodService {

    FoodItemResponse createFoodItem(FoodItemCreateRequest request);

    FoodItemResponse getFoodItemById(Long id);

    List<FoodItemResponse> getAllFoodItems();

    List<FoodItemResponse> getFoodItemsByRestaurantId(Long restaurantId);

    List<FoodItemResponse> getAvailableFoodItems();

    FoodItemResponse updateFoodItem(Long id, FoodItemUpdateRequest request);

    void deleteFoodItem(Long id);
}
