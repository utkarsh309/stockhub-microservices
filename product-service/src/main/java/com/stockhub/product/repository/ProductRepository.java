package com.stockhub.product.repository;

import com.stockhub.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Integer> {

    // Find by SKU
    Optional<Product> findBySku(String sku);

    // Check SKU exists
    boolean existsBySku(String sku);

    // All active products
    List<Product> findByIsActiveTrue();

    // Search by name
    List<Product> findByNameContainingIgnoreCase(
            String name);

    // Filter by category
    List<Product> findByCategoryAndIsActiveTrue(
            String category);
}