package com.mican.zara.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CommercialComponent {
    private long id;
    private String reference;
    private String type;
    private String kind;
    private String name;
    private String description;
    private BigDecimal price;
    private String familyName;
    private String subfamilyName;
    private ProductDetail detail;
    private Seo seo;
}
