package com.fooddelivery.order.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "food-service", url = "${services.food.url:http://localhost:8082}")
public interface FoodServiceClient {

    @GetMapping("/foods/{id}")
    FoodItemClientDto getFoodById(@PathVariable("id") Long id);
}
