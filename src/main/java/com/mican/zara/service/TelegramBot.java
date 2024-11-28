package com.mican.zara.service;

import com.mican.zara.model.Product;
import com.mican.zara.model.Size;
import com.mican.zara.model.enums.Availability;
import com.mican.zara.model.request.SubscribeRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private final String botName;
    private ProductService productService;
    private SubscriptionService subscriptionService;

    public TelegramBot(String botName, String botToken) {
        super(botToken);
        this.botName = botName;
    }

    @Autowired
    public void setProductService(ProductService productService) {
        this.productService = productService;
    }

    @Autowired
    public void setSubscriptionService(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            var chatId = message.getChatId();
            String userInput = message.getText().trim();
            log.info("message received: {}", message);

            try {
                if ("/start".equals(userInput)) {
                    execute(new SendMessage(chatId.toString(), "Zara stok botuna hoşgeldin! Ürün stok takibi için ürün kodunu yaz ve seçenekleri takip et.\n\nÖrnek format: 1255/768"));
                } else if (isValidProductCode(userInput)) {
                    // Kullanıcıdan gelen ürün koduna göre ürünleri getir
                    List<Product> products = productService.findAllByProductCode(userInput);

                    if (!products.isEmpty()) {
                        // İlk ürün detayını gönder
                        Product product = products.get(0);
                        sendProductDetails(chatId, product);

                        // Renk seçeneklerini gönder
                        List<String> colors = products.stream().map(Product::getColor).distinct().collect(Collectors.toList());
                        sendColorOptions(chatId, userInput, colors);
                    } else {
                        execute(new SendMessage(chatId.toString(), "Bu ürün koduyla eşleşen bir ürün bulunamadı."));
                    }
                } else {
                    execute(new SendMessage(chatId.toString(), "Geçersiz format! Ürün kodu şu şekilde olmalıdır: 1255/768"));
                }
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            try {
                if (callbackData.startsWith("color_")) {
                    String[] dataParts = callbackData.split("_");
                    String productCode = dataParts[1];
                    String color = dataParts[2];
                    sendSizeOptions(chatId, productCode, color);
                } else if (callbackData.startsWith("size_")) {
                    String[] dataParts = callbackData.split("_");
                    String productCode = dataParts[1];
                    String color = dataParts[2];
                    String size = dataParts[3];
                    String availability = dataParts[4];

                    processSubscription(chatId, productCode, color, size, availability);
                    execute(new SendMessage(chatId.toString(), "Başka bir ürün takip isteğiniz varsa iletebilirsiniz!"));
                }
            } catch (TelegramApiException e) {
                log.error(e.getMessage());
            }
        }
    }

    private void sendProductDetails(Long chatId, Product product) throws TelegramApiException {
        SendPhoto photoMessage = new SendPhoto();
        photoMessage.setChatId(chatId.toString());

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            photoMessage.setPhoto(new InputFile(product.getImageUrl())); // URL'yi InputFile ile sarmalıyoruz
        } else {
            photoMessage.setPhoto(new InputFile("https://via.placeholder.com/300")); // Varsayılan placeholder resmi
        }

        StringBuilder caption = new StringBuilder();
        caption.append("Ürün: ").append(product.getName()).append("\n");

        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            caption.append("Açıklama: ").append(product.getDescription()).append("\n");
        }

        if (product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            caption.append("Fiyat: ").append(product.getPrice()).append("₺\n");
        }

        photoMessage.setCaption(caption.toString());

        execute(photoMessage);
    }

    private void sendColorOptions(Long chatId, String productCode, List<String> colors) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Lütfen bir renk seçin:");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = colors.stream()
                .map(color -> {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(color); // Kullanıcıya gösterilecek renk
                    button.setCallbackData("color_" + productCode + "_" + color); // Ürün kodu ve renk callback verisine dahil
                    return List.of(button);
                })
                .toList();

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        execute(message);
    }

    private void sendSizeOptions(Long chatId, String productCode, String color) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Lütfen bir beden seçin:");

        // Belirli ürün kodu ve renge göre bedenleri alın ve uygun olanları filtreleyin
        List<Size> sizes = productService.findSizesByProductCodeAndColor(productCode, color);
        List<Size> filteredSizes = sizes.stream()
                .filter(size -> size.getAvailability() != Availability.IN_STOCK)
                .toList();

        if (filteredSizes.isEmpty()) {
            execute(new SendMessage(chatId.toString(), "Muhtemelen tüm bedenleri stokta (bi kontrol et derim)."));
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = filteredSizes.stream()
                .map(size -> {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(size.getName() + " (" + size.getAvailability().toString() + ")");
                    button.setCallbackData("size_" + productCode + "_" + color + "_" + size.getName() + "_" + size.getAvailability().toString());
                    return List.of(button);
                })
                .toList();

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        execute(message);
    }

    private void processSubscription(Long chatId, String productCode, String color, String size, String availability) {
        log.info("Kullanıcı {} için abonelik başlatılıyor. Ürün kodu: {}, Renk: {}, Beden: {}", chatId, productCode, color, size);

        SubscribeRequest subscribeRequest = new SubscribeRequest();
        subscribeRequest.setChatId(chatId.toString());
        subscribeRequest.setProductCode(productCode);
        subscribeRequest.setColor(color);
        subscribeRequest.setSize(size);
        subscribeRequest.setAvailability(availability); // Enum değerine dönüştürerek ekliyoruz

        boolean isSubscribed = subscriptionService.subscribeProduct(subscribeRequest);

        if (isSubscribed) {
            try {
                execute(new SendMessage(chatId.toString(), "Abonelik başarıyla oluşturuldu! Ürün kodu: " + productCode + ", Renk: " + color + ", Beden: " + size));
            } catch (TelegramApiException e) {
                log.error("Abonelik başarı mesajı gönderilirken hata oluştu: {}", e.getMessage());
            }
        } else {
            try {
                execute(new SendMessage(chatId.toString(), "Bu ürüne zaten abonesiniz! Ürün kodu: " + productCode + ", Renk: " + color + ", Beden: " + size));
            } catch (TelegramApiException e) {
                log.error("Zaten abone mesajı gönderilirken hata oluştu: {}", e.getMessage());
            }
        }
    }

    private boolean isValidProductCode(String input) {
        String pattern = "^\\d+/\\d+$";
        return input.matches(pattern);
    }

    @Override
    public String getBotUsername() {
        return this.botName;
    }
}
