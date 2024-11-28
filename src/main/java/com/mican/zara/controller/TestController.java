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
        // Kategorileri çek ve kaydet
        categoryService.fetchAndSaveCategories();
        System.out.println("Kategoriler güncellendi.");

        // Alt kategorilerin API ID'lerini al
        List<Long> subcategoryApiIds = categoryService.findLeafCategoryApiIds();

        // Alt kategoriler için ürünleri çek ve kaydet
        for (Long apiId : subcategoryApiIds) {
            System.out.println("Ürünler çekiliyor. Kategori API ID: " + apiId);
            productService.fetchAndSaveProductsForCategory(apiId);
        }

        System.out.println("Tüm ürünler güncellendi.");
    }

    @GetMapping("/update-product")
    public void getProducts() {
        subscriptionService.checkAvailabilityChange();
    }
}

