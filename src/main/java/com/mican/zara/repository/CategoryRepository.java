package com.mican.zara.repository;

import com.mican.zara.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c.id FROM Category c")
    List<Long> findAllCategoryIds();

    Optional<Category> findByApiId(Long apiId);

    @Query("SELECT c.apiId FROM Category c WHERE c.hasSubcategories = false")
    List<Long> findLeafCategoryApiIds();
}