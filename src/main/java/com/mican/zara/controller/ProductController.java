package com.mican.zara.controller;

import com.mican.zara.model.Product;
import com.mican.zara.model.request.SubscribeRequest;
import com.mican.zara.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/get-all")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/get-all-pageable")
    public ResponseEntity<Page<Product>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(productService.getAllProductsPageable(pageable));
    }

    @GetMapping("/search-by-code")
    public ResponseEntity<List<Product>> getProductByCode(@RequestParam("code") String code) {
        return ResponseEntity.ok(productService.findAllByProductCode(code));
    }
}
