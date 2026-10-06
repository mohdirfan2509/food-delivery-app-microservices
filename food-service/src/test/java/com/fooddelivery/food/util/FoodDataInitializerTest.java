package com.fooddelivery.food.util;

import com.fooddelivery.food.entity.FoodItem;
import com.fooddelivery.food.entity.Restaurant;
import com.fooddelivery.food.repository.FoodItemRepository;
import com.fooddelivery.food.repository.RestaurantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodDataInitializerTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @InjectMocks
    private FoodDataInitializer dataInitializer;

    @Test
    @DisplayName("Should seed initial restaurant and food data when table is empty")
    void testSeedWhenEmpty() {
        when(restaurantRepository.count()).thenReturn(0L);

        Restaurant dummyR = new Restaurant("R", "A", "P", true);
        dummyR.setRestaurantId(1L);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(dummyR);

        dataInitializer.run();

        verify(restaurantRepository, times(2)).save(any(Restaurant.class));
        verify(foodItemRepository, times(5)).save(any(FoodItem.class));
    }

    @Test
    @DisplayName("Should skip seeding when restaurants already exist")
    void testSkipWhenNotEmpty() {
        when(restaurantRepository.count()).thenReturn(2L);

        dataInitializer.run();

        verify(restaurantRepository, never()).save(any(Restaurant.class));
        verify(foodItemRepository, never()).save(any(FoodItem.class));
    }
}
