package com.yuna.yunashop.order.outbox;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

/**
 * Point d'entrée du pattern Transactional Outbox : on n'envoie jamais directement sur Kafka,
 * on enregistre l'événement dans la transaction métier et {@link OutboxRelay} le publie ensuite.
 * Si la transaction échoue, l'événement disparaît avec elle ; si Kafka est indisponible, il attend.
 */
@Component
public class Outbox {

    private final OutboxRepository repository;

    Outbox(OutboxRepository repository) {
        this.repository = repository;
    }

    /** Doit être appelé dans la transaction qui modifie l'agrégat. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String topic, UUID aggregateId, SpecificRecord event) {
        repository.save(new OutboxMessage(aggregateId, topic, aggregateId.toString(),
                event.getClass().getName(), serialize(event)));
    }

    /**
     * Supprime les messages de l'agrégat pas encore publiés sur ce topic.
     *
     * @return le nombre de messages supprimés (0 s'ils ont déjà été publiés)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public int discardPending(String topic, UUID aggregateId) {
        return repository.deleteByAggregateIdAndTopic(aggregateId, topic);
    }

    private static byte[] serialize(SpecificRecord event) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(out, null);
        try {
            new SpecificDatumWriter<SpecificRecord>(event.getSchema()).write(event, encoder);
            encoder.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot serialize " + event.getSchema().getFullName(), e);
        }
        return out.toByteArray();
    }
}
