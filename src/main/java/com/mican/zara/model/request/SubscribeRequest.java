package com.mican.zara.model.request;

import lombok.Data;

@Data
public class SubscribeRequest {
    private String size;
    private String chatId;
    private String productCode;
    private String color;
    private String availability;
}
