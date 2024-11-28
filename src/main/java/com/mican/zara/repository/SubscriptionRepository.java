package com.mican.zara.repository;

import com.mican.zara.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    boolean existsByChatIdAndProductCodeAndColorAndSize(String chatId, String productCode, String color, String size);
}
