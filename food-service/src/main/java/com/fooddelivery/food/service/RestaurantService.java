package com.fooddelivery.food.service;

import com.fooddelivery.food.dto.RestaurantCreateRequest;
import com.fooddelivery.food.dto.RestaurantResponse;
import com.fooddelivery.food.dto.RestaurantUpdateRequest;

import java.util.List;

public interface RestaurantService {

    RestaurantResponse createRestaurant(RestaurantCreateRequest request);

    RestaurantResponse getRestaurantById(Long id);

    List<RestaurantResponse> getAllRestaurants();

    RestaurantResponse updateRestaurant(Long id, RestaurantUpdateRequest request);

    void deleteRestaurant(Long id);
}
