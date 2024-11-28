package com.mican.zara.service;

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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");

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
                    log.info("Telegram gönderiliyor");
                    sendTelegramNotification(subscription, matchingSize); // Telegram bildirim fonksiyonu

                    // Abonelik veritabanından silinir
                    log.info("Abonelik siliniyor: {}", subscription);
                    subscriptionRepository.delete(subscription);
                    continue; // Abonelik silindiği için döngünün bir sonraki iterasyonuna geç
                }

                // Abonelik durumunu güncelle
                subscription.setLastAvailability(currentAvailability);
            }

            // Abonelik tarihi kontrolü: 21 günden eskiyse sil
            if (subscription.getSubscriptionDate().isBefore(ZonedDateTime.now().minusDays(21))) {
                log.info("Abonelik süresi dolduğu için siliniyor: {}", subscription);
                subscriptionRepository.delete(subscription);
            } else {
                // Güncellenme tarihini kaydet
                subscription.setLastUpdate(ZonedDateTime.now());
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

    public void sendWeeklySubscriptionSummary() {
        List<Subscription> subscriptions = getAllSubscriptions();

        // Kullanıcıların aboneliklerini gruplandır
        Map<String, List<Subscription>> subscriptionsByChatId = subscriptions.stream()
                .collect(Collectors.groupingBy(Subscription::getChatId));

        // Her bir kullanıcı için mesaj oluştur ve gönder
        subscriptionsByChatId.forEach((chatId, userSubscriptions) -> {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("📜 Güncel Abonelik Listesi:\n\n");

            for (Subscription subscription : userSubscriptions) {
                messageBuilder.append("🛒 Ürün Kodu: ").append(subscription.getProductCode()).append("\n")
                        .append("📦 Ürün İsmi: ").append(productService.getProductName(subscription.getProductCode(), subscription.getColor())).append("\n")
                        .append("🎨 Renk: ").append(subscription.getColor()).append("\n")
                        .append("📏 Beden: ").append(subscription.getSize()).append("\n")
                        .append("⏰ Abonelik Tarihi: ").append(subscription.getSubscriptionDate().format(DATE_FORMATTER)).append("\n\n");
            }

            // Kullanıcıya mesaj gönder
            try {
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText(messageBuilder.toString());

                TelegramBot telegramBot = new TelegramBot(botName, botToken);

                telegramBot.execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Mesaj gönderilirken hata oluştu: {}", e.getMessage());
            }
        });
    }

    public void sendUserSubscriptionList(Long chatId) {
        List<Subscription> userSubscriptions = getAllSubscriptions()
                .stream()
                .filter(subscription -> subscription.getChatId().equals(chatId.toString()))
                .toList();

        TelegramBot telegramBot = new TelegramBot(botName, botToken);

        if (userSubscriptions.isEmpty()) {
            try {
                telegramBot.execute(new SendMessage(chatId.toString(), "Henüz abonelik oluşturmadınız."));
                return;
            } catch (TelegramApiException e) {
                log.error("Boş abonelik mesajı gönderilirken hata oluştu: {}", e.getMessage());
                return;
            }
        }

        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("📜 Güncel Abonelik Listesi:\n\n");

        for (Subscription subscription : userSubscriptions) {
            messageBuilder.append("🛒 Ürün Kodu: ").append(subscription.getProductCode()).append("\n")
                    .append("📦 Ürün İsmi: ").append(productService.getProductName(subscription.getProductCode(), subscription.getColor())).append("\n")
                    .append("🎨 Renk: ").append(subscription.getColor()).append("\n")
                    .append("📏 Beden: ").append(subscription.getSize()).append("\n")
                    .append("⏰ Abonelik Tarihi: ").append(subscription.getSubscriptionDate().format(DATE_FORMATTER)).append("\n\n");
        }

        try {
            telegramBot.execute(new SendMessage(chatId.toString(), messageBuilder.toString()));
        } catch (TelegramApiException e) {
            log.error("Abonelik listesi mesajı gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }
}
