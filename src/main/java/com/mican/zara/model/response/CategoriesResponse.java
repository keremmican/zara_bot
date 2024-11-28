package com.mican.zara.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mican.zara.model.Category;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoriesResponse {
    private List<Category> categories;
}