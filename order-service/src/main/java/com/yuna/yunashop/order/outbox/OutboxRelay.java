package com.yuna.yunashop.order.outbox;

import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publie sur Kafka les messages de la table outbox (relais par polling).
 * <p>
 * Livraison "at-least-once" : si l'instance s'arrête entre l'envoi et la suppression, le message
 * est republié au prochain passage. Les consommateurs doivent donc être idempotents.
 */
@Component
class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final int batchSize;
    private final Duration sendTimeout;

    OutboxRelay(OutboxRepository repository, KafkaTemplate<String, Object> kafkaTemplate,
                @Value("${yunashop.outbox.batch-size}") int batchSize,
                @Value("${yunashop.outbox.send-timeout}") Duration sendTimeout) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.sendTimeout = sendTimeout;
    }

    @Scheduled(fixedDelayString = "${yunashop.outbox.poll-interval}")
    @Transactional
    public void publishPending() {
        List<OutboxMessage> published = new ArrayList<>();
        for (OutboxMessage message : repository.lockOldest(batchSize)) {
            if (!publish(message)) {
                // On s'arrête au premier échec pour ne pas publier les messages suivants
                // d'une même commande avant celui-ci. Il sera retenté au prochain passage.
                break;
            }
            published.add(message);
        }
        repository.deleteAllInBatch(published);
    }

    private boolean publish(OutboxMessage message) {
        try {
            // Attente synchrone : le message n'est supprimé que si le broker l'a bien reçu.
            kafkaTemplate.send(message.getTopic(), message.getMessageKey(), deserialize(message))
                    .get(sendTimeout.toMillis(), TimeUnit.MILLISECONDS);
            log.debug("Published outbox message {} to {}", message.getId(), message.getTopic());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Cannot publish outbox message {} to {}, will retry: {}",
                    message.getId(), message.getTopic(), e.getMessage());
            return false;
        }
    }

    private static SpecificRecord deserialize(OutboxMessage message) throws ClassNotFoundException, IOException {
        Class<? extends SpecificRecord> type = Class.forName(message.getEventType()).asSubclass(SpecificRecord.class);
        return new SpecificDatumReader<>(type)
                .read(null, DecoderFactory.get().binaryDecoder(message.getPayload(), null));
    }
}
