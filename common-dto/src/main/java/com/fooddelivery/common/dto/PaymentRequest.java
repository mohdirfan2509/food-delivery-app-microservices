package com.fooddelivery.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class PaymentRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Order ID is required")
    private Long orderId;

    private Long customerId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;

    public PaymentRequest() {
    }

    public PaymentRequest(Long orderId, BigDecimal amount) {
        this.orderId = orderId;
        this.amount = amount;
    }

    public PaymentRequest(Long orderId, Long customerId, BigDecimal amount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentRequest that = (PaymentRequest) o;
        return Objects.equals(orderId, that.orderId) &&
               Objects.equals(customerId, that.customerId) &&
               Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, amount);
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", amount=" + amount +
                '}';
    }
}
