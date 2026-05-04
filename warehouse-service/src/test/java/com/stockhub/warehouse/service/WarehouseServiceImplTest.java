package com.stockhub.warehouse.service;

import com.stockhub.warehouse.client.MovementClient;
import com.stockhub.warehouse.dto.*;
import com.stockhub.warehouse.entity.StockLevel;
import com.stockhub.warehouse.entity.Warehouse;
import com.stockhub.warehouse.exception.InsufficientStockException;
import com.stockhub.warehouse.exception.WarehouseNotFoundException;
import com.stockhub.warehouse.repository.StockLevelRepository;
import com.stockhub.warehouse.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private MovementClient movementClient;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private Warehouse warehouse;
    private StockLevel stockLevel;
    private WarehouseRequest warehouseRequest;
    private StockUpdateRequest stockRequest;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder()
                .warehouseId(1)
                .name("Main Warehouse")
                .location("Mumbai")
                .capacity(1000)
                .isActive(true)
                .build();

        stockLevel = StockLevel.builder()
                .stockId(1)
                .warehouseId(1)
                .productId(1)
                .quantity(100)
                .reservedQuantity(10)
                .build();

        warehouseRequest = new WarehouseRequest();
        warehouseRequest.setName("Main Warehouse");
        warehouseRequest.setLocation("Mumbai");
        warehouseRequest.setCapacity(1000);
        warehouseRequest.setManagerId(1);

        stockRequest = new StockUpdateRequest();
        stockRequest.setWarehouseId(1);
        stockRequest.setProductId(1);
        stockRequest.setQuantity(20);
        stockRequest.setPerformedBy(1);
    }

    // ─── Warehouse Tests ───────────────────────

    @Test
    void createWarehouse_success() {
        when(warehouseRepository.save(
                any(Warehouse.class)))
                .thenReturn(warehouse);

        WarehouseResponse response =
                warehouseService.createWarehouse(
                        warehouseRequest);

        assertThat(response).isNotNull();
        assertThat(response.getName())
                .isEqualTo("Main Warehouse");
        verify(warehouseRepository)
                .save(any(Warehouse.class));
    }

    @Test
    void getWarehouseById_success() {
        when(warehouseRepository.findById(1))
                .thenReturn(Optional.of(warehouse));

        WarehouseResponse response =
                warehouseService.getWarehouseById(1);

        assertThat(response).isNotNull();
        assertThat(response.getWarehouseId())
                .isEqualTo(1);
    }

    @Test
    void getWarehouseById_notFound_throwsException() {
        when(warehouseRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                warehouseService.getWarehouseById(99))
                .isInstanceOf(
                        WarehouseNotFoundException.class);
    }

    @Test
    void getAllWarehouses_success() {
        when(warehouseRepository.findByIsActiveTrue())
                .thenReturn(List.of(warehouse));

        List<WarehouseResponse> result =
                warehouseService.getAllWarehouses();

        assertThat(result).hasSize(1);
    }

    @Test
    void deactivateWarehouse_success() {
        when(warehouseRepository.findById(1))
                .thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(
                any(Warehouse.class)))
                .thenReturn(warehouse);

        warehouseService.deactivateWarehouse(1);

        assertThat(warehouse.isActive()).isFalse();
        verify(warehouseRepository)
                .save(any(Warehouse.class));
    }

    @Test
    void activateWarehouse_success() {
        warehouse.setActive(false);
        when(warehouseRepository.findById(1))
                .thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(
                any(Warehouse.class)))
                .thenReturn(warehouse);

        warehouseService.activateWarehouse(1);

        assertThat(warehouse.isActive()).isTrue();
    }

    // ─── Stock Tests ───────────────────────────

    @Test
    void addStock_success() {
        when(stockLevelRepository
                .findByWarehouseIdAndProductId(1, 1))
                .thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(
                any(StockLevel.class)))
                .thenReturn(stockLevel);

        StockLevelResponse response =
                warehouseService.addStock(stockRequest);

        assertThat(response).isNotNull();
        // 100 + 20 = 120
        assertThat(stockLevel.getQuantity())
                .isEqualTo(120);
        verify(movementClient)
                .recordMovement(any());
    }

    @Test
    void addStock_newStockRecord_createsNew() {
        when(stockLevelRepository
                .findByWarehouseIdAndProductId(1, 1))
                .thenReturn(Optional.empty());
        when(stockLevelRepository.save(
                any(StockLevel.class)))
                .thenReturn(stockLevel);

        StockLevelResponse response =
                warehouseService.addStock(stockRequest);

        assertThat(response).isNotNull();
        verify(stockLevelRepository)
                .save(any(StockLevel.class));
    }

    @Test
    void deductStock_success() {
        when(stockLevelRepository
                .findByWarehouseIdAndProductId(1, 1))
                .thenReturn(Optional.of(stockLevel));
        when(stockLevelRepository.save(
                any(StockLevel.class)))
                .thenReturn(stockLevel);

        StockLevelResponse response =
                warehouseService.deductStock(stockRequest);

        assertThat(response).isNotNull();
        // available = 100-10 = 90, deduct 20 = 80
        assertThat(stockLevel.getQuantity())
                .isEqualTo(80);
    }

    @Test
    void deductStock_insufficientStock_throwsException() {
        // available = quantity - reserved = 100-10 = 90
        // request quantity = 95 > 90
        stockRequest.setQuantity(95);

        when(stockLevelRepository
                .findByWarehouseIdAndProductId(1, 1))
                .thenReturn(Optional.of(stockLevel));

        assertThatThrownBy(() ->
                warehouseService.deductStock(stockRequest))
                .isInstanceOf(
                        InsufficientStockException.class)
                .hasMessageContaining(
                        "Insufficient stock");
    }

    @Test
    void getStockLevel_success() {
        when(stockLevelRepository
                .findByWarehouseIdAndProductId(1, 1))
                .thenReturn(Optional.of(stockLevel));

        StockLevelResponse response =
                warehouseService.getStockLevel(1, 1);

        assertThat(response).isNotNull();
        assertThat(response.getQuantity())
                .isEqualTo(100);
    }

    @Test
    void getStockByWarehouse_success() {
        when(stockLevelRepository.findByWarehouseId(1))
                .thenReturn(List.of(stockLevel));

        List<StockLevelResponse> result =
                warehouseService.getStockByWarehouse(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void getStockByProduct_success() {
        when(stockLevelRepository.findByProductId(1))
                .thenReturn(List.of(stockLevel));

        List<StockLevelResponse> result =
                warehouseService.getStockByProduct(1);

        assertThat(result).hasSize(1);
    }

    @Test
    void transferStock_sameWarehouse_throwsException() {
        StockTransferRequest transferRequest =
                new StockTransferRequest();
        transferRequest.setSourceWarehouseId(1);
        transferRequest.setDestinationWarehouseId(1);
        transferRequest.setProductId(1);
        transferRequest.setQuantity(10);

        assertThatThrownBy(() ->
                warehouseService.transferStock(
                        transferRequest))
                .isInstanceOf(
                        InsufficientStockException.class)
                .hasMessageContaining(
                        "Source and destination");
    }
}