package com.example.paymentservice.template;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Covenant {
    private Long covenantId;
    private Long userId;
    private Long departmentId;
    private String fromDate;
    private String toDate;
    private Long cost;

    public Covenant(Long covenantId, Long userId, Long departmentId, String fromDate, String toDate, Long cost) {
        this.covenantId = covenantId;
        this.userId = userId;
        this.departmentId = departmentId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.cost = cost;
    }
}
