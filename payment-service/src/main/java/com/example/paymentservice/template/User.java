package com.example.paymentservice.template;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String first_name;
    private String last_name;
    private String email;
    private String phone;
    private String date_of_birth;
    private String gender;
}