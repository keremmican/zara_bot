package com.mican.zara.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mican.zara.model.ProductGroup;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductGroupsResponse {
    private List<ProductGroup> productGroups;
}
