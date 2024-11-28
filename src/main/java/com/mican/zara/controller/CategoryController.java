package com.mican.zara.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mican.zara.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/category")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping("/get-all")
    public void getCategories() throws JsonProcessingException {
        categoryService.fetchAndSaveCategories();
    }
}
