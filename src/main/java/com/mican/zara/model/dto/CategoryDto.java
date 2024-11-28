package com.mican.zara.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoryDto {
    private long id;
    private String name;
    private String sectionName;
    private List<CategoryDto> subcategories;
    private String layout;
    private String contentType;
    private String gridLayout;
    //private Seo seo;
    //private Attributes attributes;
    private String key;
    private boolean isRedirected;
    private boolean isSelected;
    private boolean hasSubcategories;
    private boolean irrelevant;
    private int menuLevel;
}
