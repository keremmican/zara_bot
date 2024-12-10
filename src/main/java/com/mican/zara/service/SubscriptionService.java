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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

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

        // Ürün bilgisini getir
        Product product = productService.findByProductCodeAndColor(productCode, color);

        if (product == null) {
            log.warn("Ürün bulunamadı: ProductCode={}, Color={}", productCode, color);
            return false;
        }

        // Ürün linki ve ismi ayarla
        String productLink = product.getProductLink();
        String productName = product.getName();

        Subscription subscription = new Subscription();
        subscription.setProductCode(productCode);
        subscription.setSize(size);
        subscription.setChatId(chatId);
        subscription.setColor(color);
        subscription.setSubscriptionDate(ZonedDateTime.now());
        subscription.setLastAvailability(request.getAvailability());
        subscription.setProductLink(productLink);
        subscription.setProductName(productName);

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
                            "Ürün Adı: %s\n" +
                            "Ürün Kodu: %s\n" +
                            "Renk: %s\n" +
                            "Beden: %s\n" +
                            "Durum: %s\n\n" +
                            "Ürün Linki: %s\n\n" +
                            "Hemen almak için Zara'yı ziyaret edin!\n\n" +
                            "Bu abonelik için bir işlem seçin:",
                    subscription.getProductName(),
                    subscription.getProductCode(),
                    subscription.getColor(),
                    size.getName(),
                    size.getAvailability(),
                    subscription.getProductLink()
            );

            InlineKeyboardButton continueButton = new InlineKeyboardButton();
            continueButton.setText("Aboneliğe devam et");
            continueButton.setCallbackData("continue_" + subscription.getId());

            InlineKeyboardButton cancelButton = new InlineKeyboardButton();
            cancelButton.setText("Aboneliği sonlandır");
            cancelButton.setCallbackData("cancel_" + subscription.getId());

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = List.of(List.of(continueButton, cancelButton));
            markup.setKeyboard(rows);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setReplyMarkup(markup);

            TelegramBot telegramBot = new TelegramBot(botName, botToken);

            telegramBot.execute(sendMessage);

            // Aboneliği yanıt bekler duruma ayarla
            subscription.setWaitingForResponse(true);
            subscription.setSubscriptionDate(ZonedDateTime.now());
            subscriptionRepository.save(subscription);
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
                messageBuilder.append("🛒 Ürün Adı: ").append(subscription.getProductName()).append("\n")
                        .append("🛒 Ürün Kodu: ").append(subscription.getProductCode()).append("\n")
                        .append("🎨 Renk: ").append(subscription.getColor()).append("\n")
                        .append("📏 Beden: ").append(subscription.getSize()).append("\n")
                        .append("🔗 Ürün Linki: ").append(subscription.getProductLink()).append("\n")
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
            messageBuilder.append("🛒 Ürün Adı: ").append(subscription.getProductName()).append("\n")
                    .append("🛒 Ürün Kodu: ").append(subscription.getProductCode()).append("\n")
                    .append("🎨 Renk: ").append(subscription.getColor()).append("\n")
                    .append("📏 Beden: ").append(subscription.getSize()).append("\n")
                    .append("🔗 Ürün Linki: ").append(subscription.getProductLink()).append("\n")
                    .append("⏰ Abonelik Tarihi: ").append(subscription.getSubscriptionDate().format(DATE_FORMATTER)).append("\n\n");
        }

        try {
            telegramBot.execute(new SendMessage(chatId.toString(), messageBuilder.toString()));
        } catch (TelegramApiException e) {
            log.error("Abonelik listesi mesajı gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    public boolean deleteSubscription(Long subscriptionId) {
        if (subscriptionRepository.existsById(subscriptionId)) {
            subscriptionRepository.deleteById(subscriptionId);
            return true;
        }
        return false;
    }

    public void processCancelSubscription(Long chatId, String subscriptionId) {
        Subscription subscription = getSubscriptionById(Long.parseLong(subscriptionId));

        TelegramBot telegramBot = new TelegramBot(botName, botToken);

        if (subscription == null) {
            try {
                telegramBot.execute(new SendMessage(chatId.toString(), "Abonelik bulunamadı."));
            } catch (TelegramApiException e) {
                log.error("Mesaj gönderilirken hata oluştu: {}", e.getMessage());
            }
            return;
        }

        // Yanıt bekleme durumunu kaldır
        subscription.setWaitingForResponse(false);
        saveSubscription(subscription);

        // Aboneliği kaldır
        boolean deleted = deleteSubscription(subscription.getId());

        try {
            if (deleted) {
                telegramBot.execute(new SendMessage(chatId.toString(), "Abonelik başarıyla sonlandırıldı."));
            } else {
                telegramBot.execute(new SendMessage(chatId.toString(), "Abonelik bulunamadı."));
            }
        } catch (TelegramApiException e) {
            log.error("Mesaj gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    public void processContinueSubscription(Long chatId, String subscriptionId) {
        Subscription subscription = getSubscriptionById(Long.parseLong(subscriptionId));

        TelegramBot telegramBot = new TelegramBot(botName, botToken);

        if (subscription == null) {
            try {
                telegramBot.execute(new SendMessage(chatId.toString(), "Abonelik bulunamadı."));
            } catch (TelegramApiException e) {
                log.error("Mesaj gönderilirken hata oluştu: {}", e.getMessage());
            }
            return;
        }

        // Yanıt bekleme durumunu kaldır
        subscription.setWaitingForResponse(false);
        subscription.setLastUpdate(ZonedDateTime.now());
        saveSubscription(subscription);

        // Stok durumunu kontrol et
        Product updatedProduct = productService.getAndUpdateProduct(subscription);
        Size matchingSize = updatedProduct.getSizes().stream()
                .filter(size -> size.getName().equalsIgnoreCase(subscription.getSize()))
                .findFirst()
                .orElse(null);

        try {
            if (matchingSize != null &&
                    (matchingSize.getAvailability() == Availability.IN_STOCK ||
                            matchingSize.getAvailability() == Availability.LOW_ON_STOCK)) {

                // Ürün stokta
                subscription.setActive(false); // Abonelik pasif hale getiriliyor
                saveSubscription(subscription);

                telegramBot.execute(new SendMessage(chatId.toString(), "Bu ürün zaten stokta. Abonelik pasif hale getirildi."));
            } else {
                // Abonelik aktif olarak devam eder
                subscription.setActive(true);
                saveSubscription(subscription);

                telegramBot.execute(new SendMessage(chatId.toString(), "Aboneliğiniz aktif olarak devam ediyor."));
            }
        } catch (TelegramApiException e) {
            log.error("Mesaj gönderilirken hata oluştu: {}", e.getMessage());
        }
    }


    @Scheduled(fixedRate = 60000) // Her dakika çalışır
    public void checkResponseTimeouts() {
        List<Subscription> subscriptions = subscriptionRepository.findAll()
                .stream()
                .filter(Subscription::isWaitingForResponse)
                .toList();

        for (Subscription subscription : subscriptions) {
            // 2 saatlik zaman aşımı kontrolü
            if (subscription.getSubscriptionDate().isBefore(ZonedDateTime.now().minusHours(2))) {
                log.info("Yanıt bekleme süresi dolduğu için abonelik pasif hale getiriliyor: {}", subscription);

                // Kullanıcıya bilgi mesajı gönder
                notifyUserTimeout(subscription);

                // Aboneliği pasif yap
                subscription.setActive(false);
                subscription.setWaitingForResponse(false);
                saveSubscription(subscription);
            }
        }
    }


    private void notifyUserTimeout(Subscription subscription) {
        try {
            String chatId = subscription.getChatId();
            String message = String.format(
                    "⏰ *Aboneliğiniz zaman aşımına uğradı!* \n\n" +
                            "Ürün Adı: %s\n" +
                            "Ürün Kodu: %s\n" +
                            "Renk: %s\n" +
                            "Beden: %s\n\n" +
                            "Aboneliğiniz 2 saat içinde bir işlem yapılmadığı için pasif hale getirildi.",
                    subscription.getProductName(),
                    subscription.getProductCode(),
                    subscription.getColor(),
                    subscription.getSize()
            );

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText(message);
            sendMessage.setParseMode("Markdown");

            TelegramBot telegramBot = new TelegramBot(botName, botToken);

            telegramBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Zaman aşımı bildirim mesajı gönderilirken hata oluştu: {}", e.getMessage());
        }
    }

    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    public Subscription getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId).orElse(null);
    }

    public void saveSubscription(Subscription subscription) {
        subscriptionRepository.save(subscription);
    }

}
