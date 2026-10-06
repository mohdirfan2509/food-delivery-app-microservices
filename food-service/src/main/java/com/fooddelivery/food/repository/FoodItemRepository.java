package com.fooddelivery.food.repository;

import com.fooddelivery.food.entity.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {

    List<FoodItem> findByRestaurantId(Long restaurantId);

    List<FoodItem> findByAvailableTrue();

    List<FoodItem> findByRestaurantIdAndAvailableTrue(Long restaurantId);

    long countByRestaurantId(Long restaurantId);

    boolean existsByRestaurantId(Long restaurantId);
}
