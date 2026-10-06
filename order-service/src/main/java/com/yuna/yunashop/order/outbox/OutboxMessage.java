package com.yuna.yunashop.order.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement en attente de publication sur Kafka. Écrit dans la même transaction que le changement
 * métier, puis publié et supprimé par {@link OutboxRelay}.
 */
@Entity
@Table(name = "outbox")
class OutboxMessage {

    /** Croissant : sert aussi d'ordre de publication. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Commande concernée, pour pouvoir retrouver ses messages pas encore publiés. */
    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String topic;

    @Column(nullable = false)
    private String messageKey;

    /** Nom complet de la classe Avro générée, pour désérialiser {@link #payload}. */
    @Column(nullable = false)
    private String eventType;

    /** Événement encodé en Avro binaire brut (sans l'en-tête Schema Registry, ajouté à l'envoi). */
    @Column(nullable = false)
    private byte[] payload;

    @Column(nullable = false)
    private Instant createdAt;

    protected OutboxMessage() {
    }

    OutboxMessage(UUID aggregateId, String topic, String messageKey, String eventType, byte[] payload) {
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    Long getId() {
        return id;
    }

    UUID getAggregateId() {
        return aggregateId;
    }

    String getTopic() {
        return topic;
    }

    String getMessageKey() {
        return messageKey;
    }

    String getEventType() {
        return eventType;
    }

    byte[] getPayload() {
        return payload;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
