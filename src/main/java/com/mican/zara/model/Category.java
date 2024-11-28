package com.mican.zara.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Entity
@Data
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long apiId;

    private String name;

    private String sectionName;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private List<Category> subcategories;

    private String layout;
    private String contentType;
    private String gridLayout;

    private String key;
    private boolean isRedirected;
    private boolean isSelected;
    private boolean hasSubcategories;
    private boolean irrelevant;
    private int menuLevel;
}
