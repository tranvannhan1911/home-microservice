package com.example.paymentservice.controller;

import com.example.paymentservice.entity.TransactionDetails;
import com.example.paymentservice.model.PaymentRequest;
import com.example.paymentservice.model.PaymentResponse;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.template.User;
import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    private static final String PAYMENT_SERVICE = "paymentService";

    @Value("${service.user.url}")
    private String userServiceURL;

    private static Gson gson = new Gson();

    @PostMapping("/doPay/{covenantId}")
    @Retry(name = PAYMENT_SERVICE)
    public String doPayment(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, @RequestBody PaymentRequest paymentRequest, @PathVariable String covenantId) throws IOException {
        String authorization = httpServletRequest.getHeader("Authorization");
        String token = null;
        User user = null;
        String message = "Do payment in fail";
        if (null != authorization && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("text/plain");
            com.squareup.okhttp.RequestBody body = com.squareup.okhttp.RequestBody.create(mediaType, "");
            Request request = new Request.Builder()
                    .url(userServiceURL+"/api/user/info/")
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.code() == 200) {
                Long id = paymentService.doPayment(paymentRequest, covenantId);
                if (id == null) {
                    return "Not covenant with covenantId = " + covenantId;
                }
                return "Payment successful with payment id: " + id.toString() + " for covenantId = " + covenantId;
            }
        }
        return "Token in valid!";

    }

    @GetMapping("/covenant/{covenantId}")
    @Retry(name = PAYMENT_SERVICE)
    public String getPaymentDetailsByOrderId(HttpServletRequest httpServletRequest, HttpServletResponse
            httpServletResponse, @PathVariable String covenantId) throws IOException {
        String authorization = httpServletRequest.getHeader("Authorization");
        String token = null;
        User user = null;
        if (null != authorization && authorization.startsWith("Bearer ")) {
            token = authorization.substring(7);
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("text/plain");
            com.squareup.okhttp.RequestBody body = com.squareup.okhttp.RequestBody.create(mediaType, "");
            System.out.println(userServiceURL+"/api/user/info/");
            Request request = new Request.Builder()
                    .url(userServiceURL+"/api/user/info/")
                    .get()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            Response response = client.newCall(request).execute();
            if (response.code() == 200) {
                PaymentResponse paymentResponse = paymentService.getPaymentDetailsByCovenantId(covenantId);
                if (paymentResponse == null) {
                    return "No payment for covenant id: " + covenantId;
                }
                return "Payment info: " + gson.toJson(paymentResponse);
            }
        }
        return "Token in valid!";

    }

}
