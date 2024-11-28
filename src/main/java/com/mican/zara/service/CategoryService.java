package com.mican.zara.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mican.zara.model.Category;
import com.mican.zara.model.response.CategoriesResponse;
import com.mican.zara.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CategoryRepository categoryRepository;

    public void fetchAndSaveCategories() throws JsonProcessingException {
        String url = "https://www.zara.com/tr/tr/categories?ajax=true";

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        CategoriesResponse categoriesResponse = objectMapper.readValue(response.getBody(), CategoriesResponse.class);

        List<Category> categoriesToSave = new ArrayList<>();
        for (var categoryDto : categoriesResponse.getCategories()) {
            categoriesToSave.add(mapDtoToEntity(categoryDto));
        }

        categoryRepository.saveAll(categoriesToSave);
        System.out.println("Kategoriler başarıyla kaydedildi.");
    }

    private Category mapDtoToEntity(Category dto) {
        Category category = new Category();
        category.setApiId(dto.getId());
        category.setName(dto.getName());
        category.setSectionName(dto.getSectionName());
        category.setLayout(dto.getLayout());
        category.setContentType(dto.getContentType());
        category.setGridLayout(dto.getGridLayout());
        category.setKey(dto.getKey());
        category.setRedirected(dto.isRedirected());
        category.setSelected(dto.isSelected());
        category.setHasSubcategories(dto.isHasSubcategories());
        category.setIrrelevant(dto.isIrrelevant());
        category.setMenuLevel(dto.getMenuLevel());

        if (dto.getSubcategories() != null && !dto.getSubcategories().isEmpty()) {
            List<Category> subcategories = new ArrayList<>();
            for (var subDto : dto.getSubcategories()) {
                subcategories.add(mapDtoToEntity(subDto));
            }
            category.setSubcategories(subcategories);
        }
        return category;
    }

    public List<Long> getAllCategoryIds() {
        return categoryRepository.findAllCategoryIds();
    }

    public List<Long> findLeafCategoryApiIds() {
        return categoryRepository.findLeafCategoryApiIds();
    }
}
