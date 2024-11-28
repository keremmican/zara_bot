package com.mican.zara.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDetail {
    private String reference;
    private String displayReference;
    private List<ProductColor> colors;
}
