package com.fooddelivery.food.service;

import com.fooddelivery.food.dto.RestaurantCreateRequest;
import com.fooddelivery.food.dto.RestaurantResponse;
import com.fooddelivery.food.dto.RestaurantUpdateRequest;
import com.fooddelivery.food.entity.Restaurant;
import com.fooddelivery.food.exception.ConflictException;
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
public class RestaurantServiceImpl implements RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantServiceImpl.class);

    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;

    public RestaurantServiceImpl(RestaurantRepository restaurantRepository, FoodItemRepository foodItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
    }

    @Override
    @Transactional
    @CacheEvict(value = "restaurants", allEntries = true)
    public RestaurantResponse createRestaurant(RestaurantCreateRequest request) {
        log.info("Creating new restaurant with name: {}", request.getName());
        Restaurant restaurant = new Restaurant(
                request.getName(),
                request.getAddress(),
                request.getPhone(),
                true
        );
        Restaurant saved = restaurantRepository.save(restaurant);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurant", key = "#id")
    public RestaurantResponse getRestaurantById(Long id) {
        log.info("Fetching restaurant from database with id: {}", id);
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
        return mapToResponse(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "restaurants", key = "'all'")
    public List<RestaurantResponse> getAllRestaurants() {
        log.info("Fetching all restaurants from database");
        return restaurantRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurant", key = "#id"),
            @CacheEvict(value = "restaurants", allEntries = true)
    })
    public RestaurantResponse updateRestaurant(Long id, RestaurantUpdateRequest request) {
        log.info("Updating restaurant with id: {}", id);
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        restaurant.setName(request.getName());
        restaurant.setAddress(request.getAddress());
        restaurant.setPhone(request.getPhone());
        restaurant.setActive(request.getActive());

        Restaurant updated = restaurantRepository.save(restaurant);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "restaurant", key = "#id"),
            @CacheEvict(value = "restaurants", allEntries = true),
            @CacheEvict(value = "restaurantFoods", key = "#id"),
            @CacheEvict(value = "foods", allEntries = true),
            @CacheEvict(value = "availableFoods", allEntries = true)
    })
    public void deleteRestaurant(Long id) {
        log.info("Deleting restaurant with id: {}", id);
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));

        long foodCount = foodItemRepository.countByRestaurantId(id);
        if (foodCount > 0) {
            log.warn("Cannot delete restaurant {}: has {} food items", id, foodCount);
            throw new ConflictException("Cannot delete restaurant while food items exist");
        }

        restaurantRepository.delete(restaurant);
    }

    private RestaurantResponse mapToResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getRestaurantId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getActive()
        );
    }
}
