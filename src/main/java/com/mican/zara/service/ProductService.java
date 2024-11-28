package com.mican.zara.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mican.zara.model.*;
import com.mican.zara.model.dto.AvailabilityDto;
import com.mican.zara.model.dto.ColorDto;
import com.mican.zara.model.dto.ProductDto;
import com.mican.zara.model.enums.Availability;
import com.mican.zara.model.response.ProductGroupsResponse;
import com.mican.zara.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;

    public Product getAndUpdateProduct(Subscription subscription) {
        String productCode = subscription.getProductCode();
        String productColor = subscription.getColor();

        Product product = productRepository.findByProductCodeAndColor(productCode, productColor);

        if (product == null) {
            log.warn("Ürün bulunamadı: ProductCode={}, Color={}", productCode, productColor);
            return null;
        }

        String discernProductId = product.getSeoDiscernProductId();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // Availability URL oluştur

        String availabilityUrl = String.format("https://www.zara.com/tr/tr/products-details?productIds=%s&ajax=true", discernProductId);

        try {
            ResponseEntity<String> availabilityResponse = restTemplate.exchange(
                    availabilityUrl, HttpMethod.GET, entity, String.class);

            ArrayNode productArrayNode = (ArrayNode) objectMapper.readTree(availabilityResponse.getBody());

            JsonNode productNode = productArrayNode.get(0);

            JsonNode detailNode = productNode.get("detail");

            if (detailNode != null) {
                ProductDto productDto = objectMapper.treeToValue(detailNode, ProductDto.class);

                if (productDto.getDisplayReference() == null) {
                    return null;
                }

                JsonNode colorsNode = detailNode.get("colors");

                if (colorsNode != null && colorsNode.isArray()) {
                    colorsNode.forEach(colorNode -> {ColorDto colorDto;
                        try {
                            colorDto = objectMapper.treeToValue(colorNode, ColorDto.class);
                        } catch (JsonProcessingException e) {
                            return;
                        }

                        BigDecimal rawPrice = colorDto.getPrice();
                        BigDecimal price = rawPrice.divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);

                        product.setPrice(price);
                        product.setColor(colorDto.getName());

                        if (colorDto.getXmedia() != null && !colorDto.getXmedia().isEmpty()) {
                            product.setImageUrl(colorDto.getXmedia().get(0).getUrl().replace("{width}", "800"));
                        }

                        List<Size> sizes = new ArrayList<>();
                        JsonNode sizesNode = colorNode.get("sizes");
                        if (sizesNode != null) {
                            sizesNode.forEach(sizeNode -> {
                                try {
                                    AvailabilityDto availabilityDto = objectMapper.treeToValue(sizeNode, AvailabilityDto.class);

                                    Size size = new Size();
                                    size.setName(availabilityDto.getName());
                                    size.setAvailability(Availability.fromString(availabilityDto.getAvailability()));
                                    sizes.add(size);
                                } catch (Exception e) {

                                }
                            });
                        }
                        product.setSizes(sizes);

                        log.info(product.getId() == null ? "Updating product: {}" : "Updating existing product: {}", product);
                        productRepository.saveAndFlush(product);
                    });
                }
            }
        } catch (Exception ignored) {

        }

        return product;
    }

    @Async
    public void fetchAndSaveProductsForCategory(Long categoryId) {
        try {
            System.out.println("Fetching products for category: " + categoryId);

            String url = String.format("https://www.zara.com/tr/tr/category/%d/products?ajax=true", categoryId);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            ProductGroupsResponse productGroupsResponse = objectMapper.readValue(response.getBody(), ProductGroupsResponse.class);

            productGroupsResponse.getProductGroups().parallelStream().forEach(productGroup ->
                    productGroup.getElements().parallelStream().forEach(element -> {
                        List<CommercialComponent> commercialComponents = element.getCommercialComponents();

                        commercialComponents.parallelStream().forEach(component -> {
                            Seo seo = component.getSeo();
                            if (seo == null) {
                                return;
                            }

                            String keyword = seo.getKeyword();
                            String seoProductId = seo.getSeoProductId();
                            String discernProductId = seo.getDiscernProductId();

                            // Availability URL oluştur
                            String availabilityUrl = String.format("https://www.zara.com/tr/tr/%s-p%s.html?v1=%s&v2=%d&ajax=true",
                                    keyword, seoProductId, discernProductId, categoryId);

                            try {
                                ResponseEntity<String> availabilityResponse = restTemplate.exchange(
                                        availabilityUrl, HttpMethod.GET, entity, String.class);

                                JsonNode rootNode = objectMapper.readTree(availabilityResponse.getBody());
                                JsonNode productNode = rootNode.get("product");

                                JsonNode detailNode = productNode.get("detail");

                                if (detailNode != null) {
                                    ProductDto productDto = objectMapper.treeToValue(detailNode, ProductDto.class);

                                    if (productDto.getDisplayReference() == null) {
                                        return;
                                    }

                                    String productCode = productDto.getDisplayReference();

                                    JsonNode colorsNode = detailNode.get("colors");

                                    if (colorsNode != null && colorsNode.isArray()) {
                                        colorsNode.forEach(colorNode -> {ColorDto colorDto;
                                            try {
                                                colorDto = objectMapper.treeToValue(colorNode, ColorDto.class);
                                            } catch (JsonProcessingException e) {
                                                return;
                                            }

                                            Product product = productRepository.findByProductCodeAndColor(productCode, colorDto.getName()) != null
                                                    ? productRepository.findByProductCodeAndColor(productCode, colorDto.getName())
                                                    : new Product();

                                            product.setName(component.getName());
                                            product.setDescription(component.getDescription());
                                            product.setFamilyName(component.getFamilyName());
                                            product.setSubfamilyName(component.getSubfamilyName());
                                            product.setCategoryApiId(categoryId);
                                            product.setSeoKeyword(keyword);
                                            product.setSeoProductId(seoProductId);
                                            product.setSeoDiscernProductId(discernProductId);
                                            product.setProductCode(productCode);
                                            product.setColor(colorDto.getName());

                                            BigDecimal rawPrice = component.getPrice();
                                            BigDecimal price = rawPrice.divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                                            product.setPrice(price);

                                            if (colorDto.getXmedia() != null && !colorDto.getXmedia().isEmpty()) {
                                                product.setImageUrl(colorDto.getXmedia().get(0).getUrl().replace("{width}", "800"));
                                            }

                                            List<Size> sizes = new ArrayList<>();
                                            JsonNode sizesNode = colorNode.get("sizes");
                                            if (sizesNode != null) {
                                                sizesNode.forEach(sizeNode -> {
                                                    try {
                                                        AvailabilityDto availabilityDto = objectMapper.treeToValue(sizeNode, AvailabilityDto.class);

                                                        Size size = new Size();
                                                        size.setName(availabilityDto.getName());
                                                        size.setAvailability(Availability.fromString(availabilityDto.getAvailability()));
                                                        sizes.add(size);
                                                    } catch (Exception e) {

                                                    }
                                                });
                                            }
                                            product.setSizes(sizes);

                                            log.info(product.getId() == null ? "Saving new product: {}" : "Updating existing product: {}", product);
                                            productRepository.saveAndFlush(product);
                                        });
                                    }
                                }
                            } catch (Exception ignored) {

                            }
                        });
                    })
            );

        } catch (JsonProcessingException e) {
            log.error("JSON işleme hatası: ", e);
        } catch (Exception e) {
            log.error("Beklenmedik bir hata oluştu: ", e);
        }
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> findAllByProductCode(String code) {
        return productRepository.findAllByProductCode(code);
    }

    public Page<Product> getAllProductsPageable(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public List<Size> findSizesByProductCodeAndColor(String productCode, String color) {
        return productRepository.findSizesByProductCodeAndColor(productCode, color);
    }

    public String getProductName(String productCode, String color) {
        Product product = productRepository.findByProductCodeAndColor(productCode, color);
        return product != null ? product.getName() : "Bilinmiyor";
    }
}

