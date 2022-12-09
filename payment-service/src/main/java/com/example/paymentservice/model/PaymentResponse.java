package com.example.paymentservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentResponse {

    private Long paymentId;
    private String paymentStatus;
    private PaymentMode paymentMode;
    private Long amount;
    private Instant paymentDate;
    private Long covenantId;

    @Override
    public String toString() {
        return "PaymentResponse{" +
                "paymentId=" + paymentId +
                ", paymentStatus='" + paymentStatus + '\'' +
                ", paymentMode=" + paymentMode +
                ", amount=" + amount +
                ", paymentDate=" + paymentDate +
                ", covenantId=" + covenantId +
                '}';
    }
}