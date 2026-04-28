package com.stockhub.alert.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import java.util.Map;

// Calls warehouse-service
@FeignClient(name = "warehouse-service")
public interface WarehouseClient {

    // Get ALL stock items across all warehouses
    // We will manually check reorderLevel
    // per product so no filter here
    @GetMapping("/api/stock/warehouse/{warehouseId}")
    List<Map<String, Object>> getStockByWarehouse(
            @PathVariable Integer warehouseId);

    // Get all warehouses
    // So we can loop through each warehouse
    @GetMapping("/api/warehouses")
    List<Map<String, Object>> getAllWarehouses();
}