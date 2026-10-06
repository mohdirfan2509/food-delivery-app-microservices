package com.fooddelivery.food.service;

import com.fooddelivery.food.dto.FoodItemCreateRequest;
import com.fooddelivery.food.dto.FoodItemResponse;
import com.fooddelivery.food.dto.FoodItemUpdateRequest;
import com.fooddelivery.food.entity.FoodItem;
import com.fooddelivery.food.exception.ResourceNotFoundException;
import com.fooddelivery.food.repository.FoodItemRepository;
import com.fooddelivery.food.repository.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FoodServiceTest {

    @Mock
    private FoodItemRepository foodItemRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private FoodServiceImpl foodService;

    private FoodItem foodItem;

    @BeforeEach
    void setUp() {
        foodItem = new FoodItem(
                1L,
                "Butter Chicken",
                "Rich creamy gravy with chicken",
                new BigDecimal("14.99"),
                "Main Course",
                true
        );
        foodItem.setFoodId(10L);
    }

    @Test
    @DisplayName("Should create food item when restaurant exists")
    void testCreateFoodItemSuccess() {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                1L,
                "Butter Chicken",
                "Rich creamy gravy with chicken",
                new BigDecimal("14.99"),
                "Main Course",
                true
        );

        when(restaurantRepository.existsById(1L)).thenReturn(true);
        when(foodItemRepository.save(any(FoodItem.class))).thenReturn(foodItem);

        FoodItemResponse response = foodService.createFoodItem(request);

        assertNotNull(response);
        assertEquals(10L, response.getFoodId());
        assertEquals("Butter Chicken", response.getName());
        assertEquals(new BigDecimal("14.99"), response.getPrice());
        assertTrue(response.getAvailable());
        verify(foodItemRepository, times(1)).save(any(FoodItem.class));
    }

    @Test
    @DisplayName("Should reject food item creation if restaurant does not exist")
    void testCreateFoodItemInvalidRestaurant() {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                999L,
                "Butter Chicken",
                "Rich creamy gravy with chicken",
                new BigDecimal("14.99"),
                "Main Course",
                true
        );

        when(restaurantRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> foodService.createFoodItem(request));
        assertTrue(ex.getMessage().contains("Restaurant not found with id: 999"));
        verify(foodItemRepository, never()).save(any(FoodItem.class));
    }

    @Test
    @DisplayName("Should get food item by ID")
    void testGetFoodItemById() {
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(foodItem));

        FoodItemResponse response = foodService.getFoodItemById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getFoodId());
        assertEquals("Butter Chicken", response.getName());
        assertEquals(new BigDecimal("14.99"), response.getPrice());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when food item not found")
    void testGetFoodItemByIdNotFound() {
        when(foodItemRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> foodService.getFoodItemById(999L));
    }

    @Test
    @DisplayName("Should list all food items")
    void testGetAllFoodItems() {
        when(foodItemRepository.findAll()).thenReturn(List.of(foodItem));

        List<FoodItemResponse> list = foodService.getAllFoodItems();

        assertEquals(1, list.size());
        assertEquals("Butter Chicken", list.get(0).getName());
    }

    @Test
    @DisplayName("Should list food items by restaurant ID")
    void testGetFoodItemsByRestaurantId() {
        when(foodItemRepository.findByRestaurantId(1L)).thenReturn(List.of(foodItem));

        List<FoodItemResponse> list = foodService.getFoodItemsByRestaurantId(1L);

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getRestaurantId());
    }

    @Test
    @DisplayName("Should list only available food items")
    void testGetAvailableFoodItems() {
        when(foodItemRepository.findByAvailableTrue()).thenReturn(List.of(foodItem));

        List<FoodItemResponse> list = foodService.getAvailableFoodItems();

        assertEquals(1, list.size());
        assertTrue(list.get(0).getAvailable());
    }

    @Test
    @DisplayName("Should update food item")
    void testUpdateFoodItem() {
        FoodItemUpdateRequest updateRequest = new FoodItemUpdateRequest(
                "Butter Chicken Deluxe",
                "Updated description",
                new BigDecimal("16.50"),
                "Main Course",
                false
        );

        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(foodItem));
        when(foodItemRepository.save(any(FoodItem.class))).thenReturn(foodItem);

        FoodItemResponse response = foodService.updateFoodItem(10L, updateRequest);

        assertNotNull(response);
        assertEquals("Butter Chicken Deluxe", foodItem.getName());
        assertEquals(new BigDecimal("16.50"), foodItem.getPrice());
        assertFalse(foodItem.getAvailable());
        verify(foodItemRepository, times(1)).save(foodItem);
    }

    @Test
    @DisplayName("Should delete food item")
    void testDeleteFoodItem() {
        when(foodItemRepository.findById(10L)).thenReturn(Optional.of(foodItem));

        foodService.deleteFoodItem(10L);

        verify(foodItemRepository, times(1)).delete(foodItem);
    }
}
