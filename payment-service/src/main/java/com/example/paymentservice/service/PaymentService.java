package com.example.paymentservice.service;

import com.example.paymentservice.entity.TransactionDetails;
import com.example.paymentservice.model.PaymentRequest;
import com.example.paymentservice.model.PaymentResponse;

import java.util.List;

public interface PaymentService {
    Long doPayment(PaymentRequest paymentRequest, String covenantId);

    PaymentResponse getPaymentDetailsByCovenantId(String orderId);
}
