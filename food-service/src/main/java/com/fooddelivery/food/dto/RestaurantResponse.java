package com.fooddelivery.food.dto;

import java.io.Serializable;
import java.util.Objects;

public class RestaurantResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long restaurantId;
    private String name;
    private String address;
    private String phone;
    private Boolean active;

    public RestaurantResponse() {
    }

    public RestaurantResponse(Long restaurantId, String name, String address, String phone, Boolean active) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.active = active;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestaurantResponse that = (RestaurantResponse) o;
        return Objects.equals(restaurantId, that.restaurantId) &&
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(restaurantId, name);
    }

    @Override
    public String toString() {
        return "RestaurantResponse{" +
                "restaurantId=" + restaurantId +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", phone='" + phone + '\'' +
                ", active=" + active +
                '}';
    }
}
