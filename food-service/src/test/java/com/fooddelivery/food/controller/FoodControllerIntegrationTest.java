package com.fooddelivery.food.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.food.dto.FoodItemCreateRequest;
import com.fooddelivery.food.dto.FoodItemUpdateRequest;
import com.fooddelivery.food.dto.RestaurantCreateRequest;
import com.fooddelivery.food.dto.RestaurantUpdateRequest;
import com.fooddelivery.food.entity.FoodItem;
import com.fooddelivery.food.entity.Restaurant;
import com.fooddelivery.food.repository.FoodItemRepository;
import com.fooddelivery.food.repository.RestaurantRepository;
import com.fooddelivery.food.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FoodControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private Restaurant testRestaurant;
    private FoodItem testFoodItemAvailable;
    private FoodItem testFoodItemUnavailable;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        foodItemRepository.deleteAll();
        restaurantRepository.deleteAll();

        testRestaurant = new Restaurant("Tandoor Nights", "100 Curry Road", "9876543210", true);
        testRestaurant = restaurantRepository.save(testRestaurant);

        testFoodItemAvailable = new FoodItem(
                testRestaurant.getRestaurantId(),
                "Chicken Biryani",
                "Aromatic spiced basmati rice with chicken",
                new BigDecimal("13.99"),
                "Rice Dishes",
                true
        );
        testFoodItemAvailable = foodItemRepository.save(testFoodItemAvailable);

        testFoodItemUnavailable = new FoodItem(
                testRestaurant.getRestaurantId(),
                "Mutton Biryani",
                "Spiced basmati rice with tender mutton",
                new BigDecimal("16.99"),
                "Rice Dishes",
                false
        );
        testFoodItemUnavailable = foodItemRepository.save(testFoodItemUnavailable);

        adminToken = tokenProvider.generateToken(1L, "admin@foodapp.com", "ADMIN");
        customerToken = tokenProvider.generateToken(2L, "customer@foodapp.com", "CUSTOMER");
    }

    @Test
    @DisplayName("GET /restaurants - Public access returns list of restaurants")
    void testGetRestaurantsPublic() throws Exception {
        mockMvc.perform(get("/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Tandoor Nights"));
    }

    @Test
    @DisplayName("GET /restaurants/{id} - Public access returns restaurant details")
    void testGetRestaurantByIdPublic() throws Exception {
        mockMvc.perform(get("/restaurants/" + testRestaurant.getRestaurantId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tandoor Nights"))
                .andExpect(jsonPath("$.phone").value("9876543210"));
    }

    @Test
    @DisplayName("GET /foods - Public access returns all food items")
    void testGetFoodsPublic() throws Exception {
        mockMvc.perform(get("/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("GET /foods/{id} - Public access returns food item details")
    void testGetFoodByIdPublic() throws Exception {
        mockMvc.perform(get("/foods/" + testFoodItemAvailable.getFoodId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chicken Biryani"))
                .andExpect(jsonPath("$.price").value(13.99));
    }

    @Test
    @DisplayName("GET /foods/available - Public access returns only available food items")
    void testGetAvailableFoodsPublic() throws Exception {
        mockMvc.perform(get("/foods/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Chicken Biryani"))
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    @DisplayName("GET /foods/restaurant/{restaurantId} - Public access returns foods by restaurant")
    void testGetFoodsByRestaurantPublic() throws Exception {
        mockMvc.perform(get("/foods/restaurant/" + testRestaurant.getRestaurantId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("POST /restaurants - Unauthenticated returns 401 Unauthorized")
    void testCreateRestaurantUnauthenticated() throws Exception {
        RestaurantCreateRequest request = new RestaurantCreateRequest("New Place", "Address", "1234567890");

        mockMvc.perform(post("/restaurants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("POST /restaurants - Customer returns 403 Forbidden")
    void testCreateRestaurantCustomerForbidden() throws Exception {
        RestaurantCreateRequest request = new RestaurantCreateRequest("New Place", "Address", "1234567890");

        mockMvc.perform(post("/restaurants")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("POST /restaurants - Admin returns 201 Created")
    void testCreateRestaurantAdminSuccess() throws Exception {
        RestaurantCreateRequest request = new RestaurantCreateRequest("Sushi Bar", "789 Fish Way", "9876543299");

        mockMvc.perform(post("/restaurants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sushi Bar"))
                .andExpect(jsonPath("$.restaurantId").isNumber());
    }

    @Test
    @DisplayName("PUT /restaurants/{id} - Admin can update restaurant")
    void testUpdateRestaurantAdminSuccess() throws Exception {
        RestaurantUpdateRequest request = new RestaurantUpdateRequest("Tandoor Nights Renamed", "100 Curry Road", "9876543210", true);

        mockMvc.perform(put("/restaurants/" + testRestaurant.getRestaurantId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tandoor Nights Renamed"));
    }

    @Test
    @DisplayName("DELETE /restaurants/{id} - Rejects deletion when food items exist (409 Conflict)")
    void testDeleteRestaurantConflictWhenFoodItemsExist() throws Exception {
        mockMvc.perform(delete("/restaurants/" + testRestaurant.getRestaurantId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Cannot delete restaurant while food items exist"));

        assertThat(restaurantRepository.existsById(testRestaurant.getRestaurantId())).isTrue();
    }

    @Test
    @DisplayName("POST /foods - Unauthenticated returns 401 Unauthorized")
    void testCreateFoodUnauthenticated() throws Exception {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                testRestaurant.getRestaurantId(),
                "Paneer Tikka",
                "Grilled cottage cheese",
                new BigDecimal("10.99"),
                "Starters",
                true
        );

        mockMvc.perform(post("/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /foods - Customer returns 403 Forbidden")
    void testCreateFoodCustomerForbidden() throws Exception {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                testRestaurant.getRestaurantId(),
                "Paneer Tikka",
                "Grilled cottage cheese",
                new BigDecimal("10.99"),
                "Starters",
                true
        );

        mockMvc.perform(post("/foods")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /foods - Admin returns 201 Created")
    void testCreateFoodAdminSuccess() throws Exception {
        FoodItemCreateRequest request = new FoodItemCreateRequest(
                testRestaurant.getRestaurantId(),
                "Paneer Tikka",
                "Grilled cottage cheese",
                new BigDecimal("10.99"),
                "Starters",
                true
        );

        mockMvc.perform(post("/foods")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Paneer Tikka"))
                .andExpect(jsonPath("$.price").value(10.99))
                .andExpect(jsonPath("$.foodId").isNumber());
    }

    @Test
    @DisplayName("PUT /foods/{id} - Admin can update food item")
    void testUpdateFoodAdminSuccess() throws Exception {
        FoodItemUpdateRequest request = new FoodItemUpdateRequest(
                "Chicken Biryani Large",
                "Extra portion",
                new BigDecimal("15.99"),
                "Rice Dishes",
                true
        );

        mockMvc.perform(put("/foods/" + testFoodItemAvailable.getFoodId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chicken Biryani Large"))
                .andExpect(jsonPath("$.price").value(15.99));
    }

    @Test
    @DisplayName("DELETE /foods/{id} - Admin can delete food item")
    void testDeleteFoodAdminSuccess() throws Exception {
        mockMvc.perform(delete("/foods/" + testFoodItemAvailable.getFoodId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(foodItemRepository.existsById(testFoodItemAvailable.getFoodId())).isFalse();
    }

    @Test
    @DisplayName("DELETE /restaurants/{id} - Admin can delete restaurant after all foods are deleted")
    void testDeleteRestaurantAfterFoodsDeleted() throws Exception {
        foodItemRepository.deleteAll();

        mockMvc.perform(delete("/restaurants/" + testRestaurant.getRestaurantId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(restaurantRepository.existsById(testRestaurant.getRestaurantId())).isFalse();
    }
}
