package com.fooddelivery.food.service;

import com.fooddelivery.food.dto.RestaurantCreateRequest;
import com.fooddelivery.food.dto.RestaurantResponse;
import com.fooddelivery.food.dto.RestaurantUpdateRequest;
import com.fooddelivery.food.entity.Restaurant;
import com.fooddelivery.food.exception.ConflictException;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private FoodItemRepository foodItemRepository;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurant = new Restaurant(
                "Spice Garden",
                "123 Curry Lane, Downtown",
                "9876500001",
                true
        );
        restaurant.setRestaurantId(1L);
    }

    @Test
    @DisplayName("Should successfully create a restaurant")
    void testCreateRestaurant() {
        RestaurantCreateRequest request = new RestaurantCreateRequest(
                "Spice Garden",
                "123 Curry Lane, Downtown",
                "9876500001"
        );

        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);

        RestaurantResponse response = restaurantService.createRestaurant(request);

        assertNotNull(response);
        assertEquals(1L, response.getRestaurantId());
        assertEquals("Spice Garden", response.getName());
        assertTrue(response.getActive());
        verify(restaurantRepository, times(1)).save(any(Restaurant.class));
    }

    @Test
    @DisplayName("Should get restaurant by ID")
    void testGetRestaurantById() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantResponse response = restaurantService.getRestaurantById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getRestaurantId());
        assertEquals("Spice Garden", response.getName());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when restaurant does not exist")
    void testGetRestaurantByIdNotFound() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> restaurantService.getRestaurantById(99L));
    }

    @Test
    @DisplayName("Should list all restaurants")
    void testGetAllRestaurants() {
        when(restaurantRepository.findAll()).thenReturn(List.of(restaurant));

        List<RestaurantResponse> list = restaurantService.getAllRestaurants();

        assertEquals(1, list.size());
        assertEquals("Spice Garden", list.get(0).getName());
    }

    @Test
    @DisplayName("Should update restaurant")
    void testUpdateRestaurant() {
        RestaurantUpdateRequest updateRequest = new RestaurantUpdateRequest(
                "Spice Garden Updated",
                "New Address",
                "9876500099",
                false
        );

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(restaurant);

        RestaurantResponse response = restaurantService.updateRestaurant(1L, updateRequest);

        assertNotNull(response);
        assertEquals("Spice Garden Updated", restaurant.getName());
        assertEquals("New Address", restaurant.getAddress());
        assertEquals(false, restaurant.getActive());
        verify(restaurantRepository, times(1)).save(restaurant);
    }

    @Test
    @DisplayName("Should delete restaurant when no food items exist")
    void testDeleteRestaurantSuccess() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(foodItemRepository.countByRestaurantId(1L)).thenReturn(0L);

        restaurantService.deleteRestaurant(1L);

        verify(restaurantRepository, times(1)).delete(restaurant);
    }

    @Test
    @DisplayName("Should reject deleting restaurant when food items exist (Conflict 409)")
    void testDeleteRestaurantConflictWithFoodItems() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(foodItemRepository.countByRestaurantId(1L)).thenReturn(3L);

        ConflictException ex = assertThrows(ConflictException.class, () -> restaurantService.deleteRestaurant(1L));
        assertTrue(ex.getMessage().contains("Cannot delete restaurant while food items exist"));
        verify(restaurantRepository, never()).delete(any(Restaurant.class));
    }
}
