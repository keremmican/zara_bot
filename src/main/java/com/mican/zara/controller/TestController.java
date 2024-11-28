package com.mican.zara.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mican.zara.service.CategoryService;
import com.mican.zara.service.ProductService;
import com.mican.zara.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {
    private final CategoryService categoryService;
    private final ProductService productService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/update-db")
    public void getCategoriesAndProducts() throws JsonProcessingException {
        categoryService.fetchAndSaveCategories();
        List<Long> subcategoryApiIds = categoryService.findLeafCategoryApiIds();
        for (Long apiId : subcategoryApiIds) {
            productService.fetchAndSaveProductsForCategory(apiId);
        }
    }

    @GetMapping("/update-product")
    public void getProducts() {
        subscriptionService.sendWeeklySubscriptionSummary();
    }
}

