package com.yuna.yunashop.inventory.web;

import com.yuna.yunashop.inventory.domain.InventoryService;
import com.yuna.yunashop.inventory.domain.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
class ProductController {

    record ProductResponse(String id, String name, BigDecimal price, int availableQuantity) {

        static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getName(), product.getPrice(),
                    product.getAvailableQuantity());
        }
    }

    private final InventoryService inventoryService;

    ProductController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    List<ProductResponse> getProducts() {
        return inventoryService.getProducts().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductResponse> getProduct(@PathVariable String id) {
        return ResponseEntity.of(inventoryService.getProduct(id).map(ProductResponse::from));
    }
}
