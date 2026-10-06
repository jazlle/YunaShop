package com.yuna.yunashop.order.domain;

public enum OrderStatus {
    /** Commande créée, en attente de la réservation du stock et du paiement. */
    PENDING,
    /** Stock réservé et paiement accepté. */
    CONFIRMED,
    /** Stock insuffisant ou produit inconnu. */
    REJECTED,
    /** Paiement refusé (le stock réservé a été libéré) ou saga expirée : voir {@code failureReason}. */
    CANCELLED
}
