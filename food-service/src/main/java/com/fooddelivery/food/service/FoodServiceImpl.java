package com.fooddelivery.food.service;

import com.fooddelivery.food.dto.FoodItemCreateRequest;
import com.fooddelivery.food.dto.FoodItemResponse;
import com.fooddelivery.food.dto.FoodItemUpdateRequest;
import com.fooddelivery.food.entity.FoodItem;
import com.fooddelivery.food.exception.ResourceNotFoundException;
import com.fooddelivery.food.repository.FoodItemRepository;
import com.fooddelivery.food.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FoodServiceImpl implements FoodService {

    private static final Logger log = LoggerFactory.getLogger(FoodServiceImpl.class);

    private final FoodItemRepository foodItemRepository;
    private final RestaurantRepository restaurantRepository;

    public FoodServiceImpl(FoodItemRepository foodItemRepository, RestaurantRepository restaurantRepository) {
        this.foodItemRepository = foodItemRepository;
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "foods", allEntries = true),
            @CacheEvict(value = "availableFoods", allEntries = true),
            @CacheEvict(value = "restaurantFoods", key = "#request.restaurantId")
    })
    public FoodItemResponse createFoodItem(FoodItemCreateRequest request) {
        log.info("Creating food item: {} for restaurant: {}", request.getName(), request.getRestaurantId());
        if (!restaurantRepository.existsById(request.getRestaurantId())) {
            throw new ResourceNotFoundException("Restaurant not found with id: " + request.getRestaurantId());
        }

        FoodItem foodItem = new FoodItem(
                request.getRestaurantId(),
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategory(),
                request.getAvailable() != null ? request.getAvailable() : true
        );
        FoodItem saved = foodItemRepository.save(foodItem);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "food", key = "#id")
    public FoodItemResponse getFoodItemById(Long id) {
        log.info("Fetching food item from database with id: {}", id);
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));
        return mapToResponse(foodItem);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "foods", key = "'all'")
    public List<FoodItemResponse> getAllFoodItems() {
        log.info("Fetching all food items from database");
        return foodItemRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurantFoods", key = "#restaurantId")
    public List<FoodItemResponse> getFoodItemsByRestaurantId(Long restaurantId) {
        log.info("Fetching food items for restaurant from database: {}", restaurantId);
        return foodItemRepository.findByRestaurantId(restaurantId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "availableFoods", key = "'all'")
    public List<FoodItemResponse> getAvailableFoodItems() {
        log.info("Fetching available food items from database");
        return foodItemRepository.findByAvailableTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "food", key = "#id"),
            @CacheEvict(value = "foods", allEntries = true),
            @CacheEvict(value = "availableFoods", allEntries = true),
            @CacheEvict(value = "restaurantFoods", allEntries = true)
    })
    public FoodItemResponse updateFoodItem(Long id, FoodItemUpdateRequest request) {
        log.info("Updating food item with id: {}", id);
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));

        foodItem.setName(request.getName());
        foodItem.setDescription(request.getDescription());
        foodItem.setPrice(request.getPrice());
        foodItem.setCategory(request.getCategory());
        foodItem.setAvailable(request.getAvailable());

        FoodItem updated = foodItemRepository.save(foodItem);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "food", key = "#id"),
            @CacheEvict(value = "foods", allEntries = true),
            @CacheEvict(value = "availableFoods", allEntries = true),
            @CacheEvict(value = "restaurantFoods", allEntries = true)
    })
    public void deleteFoodItem(Long id) {
        log.info("Deleting food item with id: {}", id);
        FoodItem foodItem = foodItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Food item not found with id: " + id));
        foodItemRepository.delete(foodItem);
    }

    private FoodItemResponse mapToResponse(FoodItem item) {
        return new FoodItemResponse(
                item.getFoodId(),
                item.getRestaurantId(),
                item.getName(),
                item.getDescription(),
                item.getPrice(),
                item.getCategory(),
                item.getAvailable()
        );
    }
}
