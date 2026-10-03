package com.yuna.yunashop.inventory.domain;

import com.yuna.yunashop.events.InventoryFailedEvent;
import com.yuna.yunashop.events.InventoryReservedEvent;
import com.yuna.yunashop.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final ProductRepository productRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(ProductRepository productRepository, ReservationRepository reservationRepository) {
        this.productRepository = productRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Tente de réserver le stock d'une commande.
     *
     * @return un {@link InventoryReservedEvent} ou un {@link InventoryFailedEvent} à publier
     */
    @Transactional
    public Object reserve(OrderCreatedEvent order) {
        Optional<Reservation> existing = reservationRepository.findById(order.getOrderId());
        if (existing.isPresent()) {
            // Message rejoué : on republie le même résultat sans re-décrémenter le stock.
            log.warn("Order {} already reserved, replaying outcome", order.getOrderId());
            return toReservedEvent(existing.get());
        }

        Optional<Product> product = productRepository.findById(order.getProductId());
        if (product.isEmpty()) {
            return failed(order, "Unknown product " + order.getProductId());
        }
        if (!product.get().canReserve(order.getQuantity())) {
            return failed(order, "Insufficient stock for product " + order.getProductId());
        }

        product.get().reserve(order.getQuantity());
        Reservation reservation = reservationRepository.save(new Reservation(order.getOrderId(), order.getCustomerId(),
                order.getProductId(), order.getQuantity(), product.get().priceFor(order.getQuantity())));
        log.info("Reserved {} x {} for order {}", order.getQuantity(), order.getProductId(), order.getOrderId());
        return toReservedEvent(reservation);
    }

    /** Compensation : remet en stock les quantités réservées pour une commande dont le paiement a échoué. */
    @Transactional
    public void release(UUID orderId) {
        reservationRepository.findById(orderId)
                .filter(reservation -> reservation.getStatus() == Reservation.Status.RESERVED)
                .ifPresent(reservation -> {
                    productRepository.findById(reservation.getProductId())
                            .ifPresent(product -> product.release(reservation.getQuantity()));
                    reservation.markReleased();
                    log.info("Released stock reserved for order {}", orderId);
                });
    }

    @Transactional(readOnly = true)
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProduct(String productId) {
        return productRepository.findById(productId);
    }

    private static InventoryFailedEvent failed(OrderCreatedEvent order, String reason) {
        return InventoryFailedEvent.newBuilder()
                .setOrderId(order.getOrderId())
                .setReason(reason)
                .build();
    }

    private static InventoryReservedEvent toReservedEvent(Reservation reservation) {
        return InventoryReservedEvent.newBuilder()
                .setOrderId(reservation.getOrderId())
                .setCustomerId(reservation.getCustomerId())
                .setProductId(reservation.getProductId())
                .setQuantity(reservation.getQuantity())
                // Le type Avro decimal(12, 2) impose une échelle exacte de 2
                .setTotalAmount(reservation.getTotalAmount().setScale(2, RoundingMode.HALF_UP))
                .build();
    }
}
