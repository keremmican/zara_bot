package com.mican.zara.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Data
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"productCode", "color"})})
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 2048)
    private String description;

    private BigDecimal price;

    private String familyName;

    private String subfamilyName;

    private String imageUrl;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "product_id")
    private List<Size> sizes;

    private Long categoryApiId;

    private String productCode;

    private String color;

    private String seoKeyword;
    private String seoProductId;
    private String seoDiscernProductId;
}
