package com.mican.zara.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductElement {
    private String id;
    private String name;
    private String type;
    private String layout;
    private List<CommercialComponent> commercialComponents;
    private String availability;
}
