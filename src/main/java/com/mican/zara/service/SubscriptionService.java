package com.mican.zara.service;

import com.mican.zara.model.Product;
import com.mican.zara.model.Size;
import com.mican.zara.model.Subscription;
import com.mican.zara.model.enums.Availability;
import com.mican.zara.model.request.SubscribeRequest;
import com.mican.zara.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final ProductService productService;
    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String botToken;

    public boolean subscribeProduct(SubscribeRequest request) {
        String size = request.getSize();
        String chatId = request.getChatId();
        String productCode = request.getProductCode();
        String color = request.getColor();

        boolean exists = subscriptionRepository.existsByChatIdAndProductCodeAndColorAndSize(chatId, productCode, color, size);

        if (exists) {
            log.warn("Zaten mevcut bir abonelik: ChatId={}, ProductCode={}, Color={}, Size={}", chatId, productCode, color, size);
            return false;
        }

        Subscription subscription = new Subscription();
        subscription.setProductCode(productCode);
        subscription.setSize(size);
        subscription.setChatId(chatId);
        subscription.setColor(color);
        subscription.setSubscriptionDate(ZonedDateTime.now());
        subscription.setLastAvailability(request.getAvailability());

        subscriptionRepository.save(subscription);

        return true;
    }

    public void getAndUpdateAllSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        for (Subscription subscription : subscriptions) {
            productService.getAndUpdateProduct(subscription);
            subscription.setLastUpdate(ZonedDateTime.now());
            subscriptionRepository.save(subscription);
        }
    }

    public void checkAvailabilityChange() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();

        for (Subscription subscription : subscriptions) {
            // Ürüne ait bedenleri al
            List<Size> sizes = productService.findSizesByProductCodeAndColor(subscription.getProductCode(), subscription.getColor());

            // Kullanıcının seçtiği bedenin durumunu kontrol et
            Size matchingSize = sizes.stream()
                    .filter(size -> size.getName().equalsIgnoreCase(subscription.getSize()))
                    .findFirst()
                    .orElse(null);

            if (matchingSize != null) {
                String currentAvailability = matchingSize.getAvailability().toString();

                // Eğer availability değişti ve yeni durum IN_STOCK ise bildirim gönder
                if (!currentAvailability.equals(subscription.getLastAvailability()) &&
                        matchingSize.getAvailability() == Availability.IN_STOCK) {
                    log.info("Telegram gönderiliyo");
                    sendTelegramNotification(subscription, matchingSize); // Telegram bildirim fonksiyonu
                }

                // Aboneliği güncelle
                subscription.setLastAvailability(currentAvailability);
                subscriptionRepository.save(subscription);
            }
        }
    }


    private void sendTelegramNotification(Subscription subscription, Size size) {
        try {
            String chatId = subscription.getChatId();
            String message = String.format(
                    "🎉 Ürün stokta! \n\n" +
                            "Ürün Kodu: %s\n" +
                            "Renk: %s\n" +
                            "Beden: %s\n" +
                            "Durum: %s\n\n" +
                            "Hemen almak için Zara'yı ziyaret edin!",
                    subscription.getProductCode(),
                    subscription.getColor(),
                    size.getName(),
                    size.getAvailability()
            );

            // Telegram mesajını gönder
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);

            TelegramBot telegramBot = new TelegramBot(botName, botToken);

            telegramBot.execute(sendMessage);
        } catch (Exception e) {
            log.error("Telegram bildirimi gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }
}
