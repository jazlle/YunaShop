package com.yuna.yunashop.payment.messaging;

import com.yuna.yunashop.events.InventoryReservedEvent;
import com.yuna.yunashop.events.PaymentCompletedEvent;
import com.yuna.yunashop.events.PaymentFailedEvent;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaNormalization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Expose dans {@code /actuator/info} la version de chaque schéma Avro compilé dans ce service,
 * telle que connue par le Schema Registry.
 * <p>
 * Le registry ne sait pas quel schéma utilisent les consommateurs (ils ne s'y enregistrent pas) :
 * c'est donc chaque service qui le publie lui-même.
 */
@Component
class SchemaVersionInfoContributor implements InfoContributor {

    enum Role { PRODUCER, CONSUMER }

    record EventContract(String topic, Role role, Schema schema) {

        /** Sujet du registry selon la TopicNameStrategy par défaut. */
        String subject() {
            return topic + "-value";
        }
    }

    private static final List<EventContract> CONTRACTS = List.of(
            new EventContract(Topics.INVENTORY_RESERVED, Role.CONSUMER, InventoryReservedEvent.getClassSchema()),
            new EventContract(Topics.PAYMENT_COMPLETED, Role.PRODUCER, PaymentCompletedEvent.getClassSchema()),
            new EventContract(Topics.PAYMENT_FAILED, Role.PRODUCER, PaymentFailedEvent.getClassSchema()));

    private static final int NOT_FOUND = 404;

    private final SchemaRegistryClient registryClient;

    @Autowired
    SchemaVersionInfoContributor(@Value("${spring.kafka.properties.schema.registry.url}") String registryUrl) {
        this(new CachedSchemaRegistryClient(registryUrl, 100));
    }

    SchemaVersionInfoContributor(SchemaRegistryClient registryClient) {
        this.registryClient = registryClient;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> schemas = new LinkedHashMap<>();
        CONTRACTS.forEach(contract -> schemas.put(contract.subject(), describe(contract)));
        builder.withDetail("avroSchemas", schemas);
    }

    private Map<String, Object> describe(EventContract contract) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("topic", contract.topic());
        details.put("role", contract.role());
        details.put("record", contract.schema().getFullName());
        // Empreinte de la forme canonique (ignore doc, ordre des attributs...) : permet de comparer
        // les copies dupliquées entre services, même sans registry.
        details.put("fingerprint", Long.toHexString(SchemaNormalization.parsingFingerprint64(contract.schema())));

        AvroSchema localSchema = new AvroSchema(contract.schema());
        try {
            Integer latestVersion = latestVersion(contract.subject());
            Integer version = registeredVersion(contract.subject(), localSchema);
            details.put("registered", version != null);
            if (version != null) {
                details.put("version", version);
                details.put("id", registryClient.getId(contract.subject(), localSchema));
            }
            details.put("latestVersion", latestVersion);
            if (version != null) {
                details.put("upToDate", version.equals(latestVersion));
            } else if (latestVersion != null) {
                // Copie locale absente du registry : on vérifie qu'elle reste compatible.
                details.put("compatibleWithLatest", registryClient.testCompatibility(contract.subject(), localSchema));
            }
        } catch (IOException | RestClientException e) {
            details.put("error", "Schema Registry unavailable: " + e.getMessage());
        }
        return details;
    }

    private Integer latestVersion(String subject) throws IOException, RestClientException {
        try {
            return registryClient.getLatestSchemaMetadata(subject).getVersion();
        } catch (RestClientException e) {
            if (e.getStatus() == NOT_FOUND) {
                // Sujet pas encore créé : le producteur n'a encore publié aucun message.
                return null;
            }
            throw e;
        }
    }

    private Integer registeredVersion(String subject, AvroSchema schema) throws IOException, RestClientException {
        try {
            return registryClient.getVersion(subject, schema);
        } catch (RestClientException e) {
            if (e.getStatus() == NOT_FOUND) {
                return null;
            }
            throw e;
        }
    }
}
