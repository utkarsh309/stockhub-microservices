package com.stockhub.alert.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

// Calls product-service to get reorderLevel
@FeignClient(name = "product-service")
public interface ProductClient {

    // Get product details including
    // reorderLevel and maxStockLevel
    @GetMapping("/api/products/{productId}")
    Map<String, Object> getProductById(
            @PathVariable Integer productId);
}