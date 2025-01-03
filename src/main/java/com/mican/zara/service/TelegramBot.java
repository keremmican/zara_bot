package com.mican.zara.service;

import com.mican.zara.model.Product;
import com.mican.zara.model.Size;
import com.mican.zara.model.Subscription;
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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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

    private String encodeForCallback(String input) {
        return URLEncoder.encode(input, StandardCharsets.UTF_8); // UTF-8 encoding
    }


    private String decodeInput(String input) {
        return URLDecoder.decode(input, StandardCharsets.UTF_8); // UTF-8 decoding
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            var chatId = message.getChatId();
            String userInput = decodeInput(message.getText().trim()); // Gelen mesajı decode et
            log.info("Decoded message received: {}", userInput);

            try {
                if ("/start".equals(userInput)) {
                    sendWelcomeMessage(chatId);
                } else if ("/bilgi".equals(userInput)) {
                    sendUsageInfo(chatId);
                } else if ("/list".equals(userInput)) {
                    log.info("Kullanıcıdan '/list' komutu alındı: ChatId={}", chatId);
                    subscriptionService.sendUserSubscriptionList(chatId);
                } else if (isValidProductCode(userInput)) {
                    List<Product> products = productService.findAllByProductCode(userInput);

                    if (!products.isEmpty()) {
                        Product product = products.get(0);
                        sendProductDetails(chatId, product);

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
            String callbackData = decodeInput(update.getCallbackQuery().getData()); // Gelen callback data'yı decode et
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            try {
                if (callbackData.startsWith("continue_")) {
                    String subscriptionId = callbackData.split("_")[1];
                    subscriptionService.processContinueSubscription(chatId, subscriptionId);
                } else if (callbackData.startsWith("cancel_")) {
                    String subscriptionId = callbackData.split("_")[1];
                    subscriptionService.processCancelSubscription(chatId, subscriptionId);
                } else if (callbackData.startsWith("color_")) {
                    String[] dataParts = callbackData.split("_");
                    String productCode = decodeInput(dataParts[1]); // Decode edilen productCode
                    String color = decodeInput(dataParts[2]); // Decode edilen color
                    sendSizeOptions(chatId, productCode, color);
                } else if (callbackData.startsWith("size_")) {
                    String[] dataParts = callbackData.split("_");
                    Long sizeId = Long.parseLong(dataParts[1]); // sizeId'yi alıyoruz

                    processSubscription(chatId, sizeId);
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

        List<Size> sizes = productService.findSizesByProductCodeAndColor(productCode, color);

        if (sizes.isEmpty()) {
            execute(new SendMessage(chatId.toString(), "Bu renk ve ürün için beden bulunamadı."));
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rows = sizes.stream()
                .map(size -> {
                    InlineKeyboardButton button = new InlineKeyboardButton();
                    button.setText(size.getName()); // Gösterilecek beden adı
                    button.setCallbackData("size_" + size.getId()); // Sadece sizeId gönderiliyor
                    return List.of(button);
                })
                .toList();

        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        execute(message);
    }

    private void processSubscription(Long chatId, Long sizeId) {
        log.info("Kullanıcı {} için abonelik başlatılıyor. SizeId: {}", chatId, sizeId);

        // Size objesini sizeId ile al
        Size selectedSize = productService.findSizeById(sizeId); // Bu metodu ProductService'de tanımlayın

        if (selectedSize == null) {
            log.error("Size bulunamadı. SizeId: {}", sizeId);
            try {
                execute(new SendMessage(chatId.toString(), "Seçtiğiniz beden bulunamadı."));
            } catch (TelegramApiException e) {
                log.error("Hata mesajı gönderilirken hata oluştu: {}", e.getMessage());
            }
            return;
        }

        Product p = productService.getAndUpdateProductBySizeId(sizeId);
        Product updatedProduct = productService.getAndUpdateProduct(new Subscription(p.getProductCode(), p.getColor(), selectedSize.getName(), selectedSize.getAvailability().toString()));


        if (updatedProduct == null) {
            log.error("Ürün güncellenemedi veya bulunamadı. SizeId: {}", sizeId);
            try {
                execute(new SendMessage(chatId.toString(), "Ürün bulunamadı veya güncellenemedi."));
            } catch (TelegramApiException e) {
                log.error("Hata mesajı gönderilirken hata oluştu: {}", e.getMessage());
            }
            return;
        }

        // 2) Güncellenmiş product içindeki size'ı bulalım
        selectedSize = updatedProduct.getSizes().stream()
                .filter(size -> size.getId().equals(sizeId))
                .findFirst()
                .orElse(null);

        if (selectedSize == null) {
            log.error("Seçtiğiniz beden, güncellenen üründe bulunamadı. SizeId: {}", sizeId);
            try {
                execute(new SendMessage(chatId.toString(), "Seçtiğiniz beden bulunamadı."));
            } catch (TelegramApiException e) {
                log.error("Hata mesajı gönderilirken hata oluştu: {}", e.getMessage());
            }
            return;
        }

        if (selectedSize.getAvailability() != Availability.IN_STOCK &&
                selectedSize.getAvailability() != Availability.LOW_ON_STOCK) {

            log.info("Ürün stokta değil, abonelik başlatılıyor.");

            SubscribeRequest subscribeRequest = new SubscribeRequest();
            subscribeRequest.setChatId(chatId.toString());
            subscribeRequest.setProductCode(updatedProduct.getProductCode());
            subscribeRequest.setColor(updatedProduct.getColor());
            subscribeRequest.setSize(selectedSize.getName());
            subscribeRequest.setAvailability(selectedSize.getAvailability().toString());

            boolean isSubscribed = subscriptionService.subscribeProduct(subscribeRequest);

            if (isSubscribed) {
                try {
                    execute(new SendMessage(chatId.toString(), "Abonelik başarıyla oluşturuldu! Ürün kodu: " + updatedProduct.getProductCode() + ", Renk: " + updatedProduct.getColor() + ", Beden: " + selectedSize.getName()));
                } catch (TelegramApiException e) {
                    log.error("Abonelik başarı mesajı gönderilirken hata oluştu: {}", e.getMessage());
                }
            } else {
                try {
                    execute(new SendMessage(chatId.toString(), "Bu ürüne zaten abonesiniz! Ürün kodu: " + updatedProduct.getProductCode() + ", Renk: " + updatedProduct.getColor() + ", Beden: " + selectedSize.getName()));
                } catch (TelegramApiException e) {
                    log.error("Zaten abone mesajı gönderilirken hata oluştu: {}", e.getMessage());
                }
            }
        } else {
            try {
                execute(new SendMessage(chatId.toString(), "Seçtiğiniz beden zaten stokta."));
            } catch (TelegramApiException e) {
                log.error("Stok mesajı gönderilirken hata oluştu: {}", e.getMessage());
            }
        }
    }

    private void sendWelcomeMessage(Long chatId) throws TelegramApiException {
        String welcomeMessage = """
                📖 *Zara Stok Bot Kullanım Rehberi*:

                - Ürün stok takibi yapmak için ürün kodunu şu formatta yazabilirsiniz: `1255/768`
                - `/list` komutunu kullanarak mevcut aboneliklerinizi listeleyebilirsiniz.
                - Aboneliklerinizin süresi 21 gün olup, 21 gün sonra otomatik olarak silinir.
                - Stok durumu değiştiğinde size bildirim gönderilecektir.

                ✨ *Başlamak için bir ürün kodu yazın ve seçenekleri takip edin!* ✨
                """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(welcomeMessage);
        message.setParseMode("Markdown");
        execute(message);
    }

    private void sendUsageInfo(Long chatId) throws TelegramApiException {
        String usageInfo = """
                📖 *Zara Stok Bot Kullanım Rehberi*:

                - Ürün stok takibi yapmak için ürün kodunu şu formatta yazabilirsiniz: `1255/768`
                - `/list` komutunu kullanarak mevcut aboneliklerinizi listeleyebilirsiniz.
                - Aboneliklerinizin süresi 21 gün olup, 21 gün sonra otomatik olarak silinir.
                - Stok durumu değiştiğinde size bildirim gönderilecektir.

                ✨ *Başlamak için bir ürün kodu yazın ve seçenekleri takip edin!* ✨
                """;

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(usageInfo);
        message.setParseMode("Markdown");
        execute(message);
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
