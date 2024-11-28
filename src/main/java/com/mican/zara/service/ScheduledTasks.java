package com.mican.zara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final SubscriptionService subscriptionService;

    /**
     * Günde bir kez gece 03:00'te kategorileri ve ürünleri getirip kaydeder.
     */
    @Scheduled(cron = "0 0 3 * * ?") // Her gece saat 03:00
    public void fetchCategoriesAndProducts() {
        try {
            categoryService.fetchAndSaveCategories();
            categoryService.getAllCategoryIds().forEach(productService::fetchAndSaveProductsForCategory);
            log.info("Categories and products fetched and saved successfully.");
        } catch (Exception e) {
            log.error("Error fetching categories and products", e);
        }
    }

    /**
     * Her 5 dakikada bir abonelikleri günceller.
     */
    @Scheduled(fixedRate = 300000) // 5 dakika (300.000 ms)
    public void updateSubscriptions() {
        try {
            subscriptionService.getAndUpdateAllSubscriptions();
            log.info("Subscriptions updated successfully.");
        } catch (Exception e) {
            log.error("Error updating subscriptions", e);
        }
    }

    @Scheduled(cron = "0 */2 * * * *") // Her 2 dakikada bir çalışır
    public void checkSubscriptions() {
        try {
            subscriptionService.checkAvailabilityChange();
            log.info("Availability change check successfully.");
        } catch (Exception e) {
            log.error("Error checking availabilities.", e);
        }
    }
}
