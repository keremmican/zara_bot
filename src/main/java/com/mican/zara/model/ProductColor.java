package com.mican.zara.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductColor {
    private String id;
    private long productId;
    private String name;
    private String stylingId;
    private List<XMedia> xmedia;
    private int price;
    private String availability;
    private String reference;
}
