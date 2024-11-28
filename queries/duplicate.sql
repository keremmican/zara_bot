SELECT *
FROM Product p
WHERE p.product_code IN (
    SELECT product_code
    FROM Product
    GROUP BY product_code
    HAVING COUNT(id) > 1
);
