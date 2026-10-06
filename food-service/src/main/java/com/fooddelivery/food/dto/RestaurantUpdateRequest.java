package com.fooddelivery.food.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RestaurantUpdateRequest {

    @NotBlank(message = "Restaurant name is required")
    @Size(min = 2, max = 150, message = "Restaurant name must be between 2 and 150 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    private String phone;

    private Boolean active;

    public RestaurantUpdateRequest() {
    }

    public RestaurantUpdateRequest(String name, String address, String phone, Boolean active) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
