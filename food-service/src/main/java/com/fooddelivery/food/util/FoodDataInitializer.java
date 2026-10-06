package com.fooddelivery.food.util;

import com.fooddelivery.food.entity.FoodItem;
import com.fooddelivery.food.entity.Restaurant;
import com.fooddelivery.food.repository.FoodItemRepository;
import com.fooddelivery.food.repository.RestaurantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FoodDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FoodDataInitializer.class);

    private final RestaurantRepository restaurantRepository;
    private final FoodItemRepository foodItemRepository;

    public FoodDataInitializer(RestaurantRepository restaurantRepository, FoodItemRepository foodItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.foodItemRepository = foodItemRepository;
    }

    @Override
    public void run(String... args) {
        if (restaurantRepository.count() == 0) {
            log.info("Seeding initial restaurant and food data...");

            Restaurant r1 = new Restaurant(
                    "Spice Garden",
                    "123 Curry Lane, Downtown",
                    "9876500001",
                    true
            );
            Restaurant savedR1 = restaurantRepository.save(r1);

            Restaurant r2 = new Restaurant(
                    "Bella Italia",
                    "456 Pasta Ave, Uptown",
                    "9876500002",
                    true
            );
            Restaurant savedR2 = restaurantRepository.save(r2);

            foodItemRepository.save(new FoodItem(
                    savedR1.getRestaurantId(),
                    "Butter Chicken",
                    "Rich creamy tomato gravy with tender chicken",
                    new BigDecimal("14.99"),
                    "Main Course",
                    true
            ));

            foodItemRepository.save(new FoodItem(
                    savedR1.getRestaurantId(),
                    "Garlic Naan",
                    "Freshly baked clay oven flatbread with garlic",
                    new BigDecimal("3.99"),
                    "Breads",
                    true
            ));

            foodItemRepository.save(new FoodItem(
                    savedR1.getRestaurantId(),
                    "Mango Lassi",
                    "Traditional chilled yogurt mango smoothie",
                    new BigDecimal("4.50"),
                    "Beverages",
                    false
            ));

            foodItemRepository.save(new FoodItem(
                    savedR2.getRestaurantId(),
                    "Margherita Pizza",
                    "Classic wood-fired pizza with fresh basil and mozzarella",
                    new BigDecimal("12.50"),
                    "Pizza",
                    true
            ));

            foodItemRepository.save(new FoodItem(
                    savedR2.getRestaurantId(),
                    "Tiramisu",
                    "Classic Italian coffee flavoured dessert",
                    new BigDecimal("7.00"),
                    "Desserts",
                    true
            ));

            log.info("Initialized 2 seed restaurants and 5 seed food items (4 available, 1 unavailable).");
        } else {
            log.info("Restaurant data already present. Skipping initial data seeding.");
        }
    }
}
