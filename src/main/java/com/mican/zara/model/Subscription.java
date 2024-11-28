package com.mican.zara.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@Data
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String size;
    private String chatId;
    private String productCode;
    private String color;
    private String lastAvailability;
    private ZonedDateTime subscriptionDate;
    private ZonedDateTime lastUpdate;
}
