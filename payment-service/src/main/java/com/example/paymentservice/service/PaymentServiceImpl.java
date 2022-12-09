package com.example.paymentservice.service;

import com.example.paymentservice.entity.TransactionDetails;
import com.example.paymentservice.model.PaymentMode;
import com.example.paymentservice.model.PaymentRequest;
import com.example.paymentservice.model.PaymentResponse;
import com.example.paymentservice.repository.TransactionDetailsRepository;
import com.example.paymentservice.template.Covenant;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.Gson;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;

@Service
@Log4j2
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private RestTemplate restTemplate;

    
    @Value("${service.contract.url}")
    private String contractServiceURL;

    // private static final String BASE_URL
    //         = "http://localhost:9999/api/covenant/byId";
    private static Gson gson = new Gson();

    @Autowired
    private TransactionDetailsRepository transactionDetailsRepository;
    int count = 1;

    @Override
    public Long doPayment(PaymentRequest paymentRequest, String covenantId) {
        System.out.println("Retry method called " + count++ + " times at " + new Date());
        Object response = restTemplate.getForEntity(contractServiceURL + "/api/covenant/" + covenantId, Object.class).getBody();
        Covenant covenant = gson.fromJson(gson.toJson(response), Covenant.class);
        if (covenant == null) {
            return null;
        }
        log.info("Recording Payment Details: {}", paymentRequest);
        TransactionDetails transactionDetails
                = TransactionDetails.builder()
                .paymentDate(Instant.now())
                .paymentMode(paymentRequest.getPaymentMode().name())
                .paymentStatus("SUCCESS")
                .covenantId(covenant.getCovenantId())
                .referenceNumber(paymentRequest.getReferenceNumber())
                .amount(covenant.getCost())
                .build();

        transactionDetailsRepository.save(transactionDetails);

        log.info("Transaction Completed with Id: {}", transactionDetails.getPaymentId());

        return transactionDetails.getPaymentId();
    }

    @Override
    public PaymentResponse getPaymentDetailsByCovenantId(String covenantId) {
        System.out.println("Retry method called " + count++ + " times at " + new Date());
        log.info("Getting payment details for the Order Id: {}", covenantId);

        TransactionDetails transactionDetails
                = transactionDetailsRepository.findByCovenantId(Long.valueOf(covenantId));
        if (transactionDetails == null) {
            return null;
        }

        PaymentResponse paymentResponse
                = PaymentResponse.builder()
                .paymentId(transactionDetails.getPaymentId())
                .paymentMode(PaymentMode.valueOf(transactionDetails.getPaymentMode()))
                .paymentDate(transactionDetails.getPaymentDate())
                .covenantId(transactionDetails.getCovenantId())
                .paymentStatus(transactionDetails.getPaymentStatus())
                .amount(transactionDetails.getAmount())
                .build();


        return paymentResponse;
    }

}
