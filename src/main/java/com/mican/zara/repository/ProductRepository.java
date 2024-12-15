package com.mican.zara.repository;

import com.mican.zara.model.Product;
import com.mican.zara.model.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByProductCode(String productCode);
    Page<Product> findAll(Pageable pageable);
    Product findByProductCodeAndColor(String productCode, String color);

    @Query("SELECT '*' FROM Product p JOIN p.sizes s WHERE p.productCode = :productCode AND s.name = :sizeName")
    List<Product> findByProductCodeAndSizeName(@Param("productCode") String productCode, @Param("sizeName") String sizeName);

    @Query("SELECT s FROM Product p JOIN p.sizes s WHERE p.productCode = :productCode AND p.color = :color")
    List<Size> findSizesByProductCodeAndColor(String productCode, String color);

    @Query("SELECT p FROM Product p JOIN p.sizes s WHERE s.id = :sizeId")
    Product findProductBySizeId(@Param("sizeId") Long sizeId);

    @Query("SELECT p FROM Product p JOIN FETCH p.sizes s WHERE s = :size")
    Product findBySizesContaining(@Param("size") Size size);

}
