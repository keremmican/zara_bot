package com.mican.zara.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Seo {
    private String keyword;
    private String seoProductId;
    private String discernProductId;
}
