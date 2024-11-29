package com.mican.zara.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor; // Ekle
import lombok.Data;
import lombok.NoArgsConstructor; // Ekle

import java.time.ZonedDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private boolean waitingForResponse = false;
    private String productLink;
    private String productName;

    public Subscription(String productCode, String color, String size, String lastAvailability) {
        this.productCode = productCode;
        this.color = color;
        this.size = size;
        this.lastAvailability = lastAvailability;
        this.subscriptionDate = ZonedDateTime.now();
    }
}