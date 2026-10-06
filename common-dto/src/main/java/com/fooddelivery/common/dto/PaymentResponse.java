package com.fooddelivery.common.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fooddelivery.common.enums.PaymentStatus;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class PaymentResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;

    @JsonAlias({"status", "paymentStatus"})
    private PaymentStatus paymentStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime paymentDate;

    private String transactionReference;

    public PaymentResponse() {
    }

    public PaymentResponse(Long paymentId, Long orderId, BigDecimal amount,
                           PaymentStatus paymentStatus, LocalDateTime paymentDate,
                           String transactionReference) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.paymentDate = paymentDate;
        this.transactionReference = transactionReference;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentStatus getStatus() {
        return paymentStatus;
    }

    public void setStatus(PaymentStatus status) {
        this.paymentStatus = status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentResponse that = (PaymentResponse) o;
        return Objects.equals(paymentId, that.paymentId) &&
               Objects.equals(orderId, that.orderId) &&
               Objects.equals(amount, that.amount) &&
               paymentStatus == that.paymentStatus &&
               Objects.equals(paymentDate, that.paymentDate) &&
               Objects.equals(transactionReference, that.transactionReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, orderId, amount, paymentStatus, paymentDate, transactionReference);
    }

    @Override
    public String toString() {
        return "PaymentResponse{" +
                "paymentId=" + paymentId +
                ", orderId=" + orderId +
                ", amount=" + amount +
                ", paymentStatus=" + paymentStatus +
                ", paymentDate=" + paymentDate +
                ", transactionReference='" + transactionReference + '\'' +
                '}';
    }
}
