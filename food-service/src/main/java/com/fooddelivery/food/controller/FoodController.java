package com.fooddelivery.food.controller;

import com.fooddelivery.food.dto.FoodItemCreateRequest;
import com.fooddelivery.food.dto.FoodItemResponse;
import com.fooddelivery.food.dto.FoodItemUpdateRequest;
import com.fooddelivery.food.service.FoodService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/foods")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @PostMapping
    public ResponseEntity<FoodItemResponse> createFoodItem(
            @Valid @RequestBody FoodItemCreateRequest request) {
        FoodItemResponse created = foodService.createFoodItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<FoodItemResponse>> getAllFoodItems() {
        return ResponseEntity.ok(foodService.getAllFoodItems());
    }

    @GetMapping("/available")
    public ResponseEntity<List<FoodItemResponse>> getAvailableFoodItems() {
        return ResponseEntity.ok(foodService.getAvailableFoodItems());
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<FoodItemResponse>> getFoodItemsByRestaurantId(
            @PathVariable Long restaurantId) {
        return ResponseEntity.ok(foodService.getFoodItemsByRestaurantId(restaurantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodItemResponse> getFoodItemById(@PathVariable Long id) {
        return ResponseEntity.ok(foodService.getFoodItemById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FoodItemResponse> updateFoodItem(
            @PathVariable Long id,
            @Valid @RequestBody FoodItemUpdateRequest request) {
        return ResponseEntity.ok(foodService.updateFoodItem(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFoodItem(@PathVariable Long id) {
        foodService.deleteFoodItem(id);
        return ResponseEntity.noContent().build();
    }
}
