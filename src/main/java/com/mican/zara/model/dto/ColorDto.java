package com.mican.zara.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mican.zara.model.XMedia;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ColorDto {
    private String name;
    private String hexCode;
    private List<XMedia> xmedia;
    private BigDecimal price;
}
