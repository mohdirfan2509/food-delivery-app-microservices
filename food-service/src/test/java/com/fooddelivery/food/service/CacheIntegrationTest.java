package com.fooddelivery.food.service;

import com.fooddelivery.food.dto.FoodItemCreateRequest;
import com.fooddelivery.food.dto.FoodItemResponse;
import com.fooddelivery.food.dto.FoodItemUpdateRequest;
import com.fooddelivery.food.dto.RestaurantCreateRequest;
import com.fooddelivery.food.dto.RestaurantResponse;
import com.fooddelivery.food.dto.RestaurantUpdateRequest;
import com.fooddelivery.food.repository.FoodItemRepository;
import com.fooddelivery.food.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CacheIntegrationTest {

    @Autowired
    private FoodService foodService;

    @Autowired
    private RestaurantService restaurantService;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private CacheManager cacheManager;

    private Long restaurantId;
    private Long foodId;

    @BeforeEach
    void setUp() {
        foodItemRepository.deleteAll();
        restaurantRepository.deleteAll();

        // Clear all caches
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }

        RestaurantResponse r = restaurantService.createRestaurant(
                new RestaurantCreateRequest("Cache Cafe", "10 Cache St", "9876500000")
        );
        restaurantId = r.getRestaurantId();

        FoodItemResponse f = foodService.createFoodItem(
                new FoodItemCreateRequest(restaurantId, "Cache Burger", "Delicious", new BigDecimal("8.99"), "Burger", true)
        );
        foodId = f.getFoodId();
    }

    @Test
    @DisplayName("Restaurant Cache - Miss loads DB, hit uses cache, update and delete evict cache")
    void testRestaurantCaching() {
        Cache restaurantCache = cacheManager.getCache("restaurant");
        assertThat(restaurantCache).isNotNull();

        // Initially not in cache
        assertThat(restaurantCache.get(restaurantId)).isNull();

        // 1. First request: Cache MISS -> loads from DB -> populates cache
        RestaurantResponse firstFetch = restaurantService.getRestaurantById(restaurantId);
        assertThat(firstFetch).isNotNull();
        assertThat(restaurantCache.get(restaurantId)).isNotNull();

        // 2. Second request: Cache HIT
        RestaurantResponse secondFetch = restaurantService.getRestaurantById(restaurantId);
        assertThat(secondFetch.getName()).isEqualTo("Cache Cafe");

        // 3. Update restaurant: Evicts cache
        restaurantService.updateRestaurant(restaurantId,
                new RestaurantUpdateRequest("Cache Cafe Updated", "10 Cache St", "9876500000", true));
        assertThat(restaurantCache.get(restaurantId)).isNull();

        // 4. Fetch after update: re-caches new value
        RestaurantResponse updatedFetch = restaurantService.getRestaurantById(restaurantId);
        assertThat(updatedFetch.getName()).isEqualTo("Cache Cafe Updated");
        assertThat(restaurantCache.get(restaurantId)).isNotNull();

        // Delete all foods first so restaurant can be deleted
        foodService.deleteFoodItem(foodId);

        // 5. Delete restaurant: Evicts cache
        restaurantService.deleteRestaurant(restaurantId);
        assertThat(restaurantCache.get(restaurantId)).isNull();
    }

    @Test
    @DisplayName("Food Cache - Miss loads DB, hit uses cache, update and delete evict cache")
    void testFoodCaching() {
        Cache foodCache = cacheManager.getCache("food");
        assertThat(foodCache).isNotNull();

        // Initially not in cache
        assertThat(foodCache.get(foodId)).isNull();

        // 1. First request: Cache MISS -> loads DB -> caches
        FoodItemResponse firstFetch = foodService.getFoodItemById(foodId);
        assertThat(firstFetch).isNotNull();
        assertThat(foodCache.get(foodId)).isNotNull();

        // 2. Second request: Cache HIT
        FoodItemResponse secondFetch = foodService.getFoodItemById(foodId);
        assertThat(secondFetch.getName()).isEqualTo("Cache Burger");

        // 3. Update food item: Evicts cache
        foodService.updateFoodItem(foodId,
                new FoodItemUpdateRequest("Cache Burger Deluxe", "Extra cheese", new BigDecimal("10.99"), "Burger", true));
        assertThat(foodCache.get(foodId)).isNull();

        // 4. Subsequent fetch loads updated item and caches
        FoodItemResponse updatedFetch = foodService.getFoodItemById(foodId);
        assertThat(updatedFetch.getPrice()).isEqualByComparingTo("10.99");
        assertThat(foodCache.get(foodId)).isNotNull();

        // 5. Delete food item: Evicts cache
        foodService.deleteFoodItem(foodId);
        assertThat(foodCache.get(foodId)).isNull();
    }
}
