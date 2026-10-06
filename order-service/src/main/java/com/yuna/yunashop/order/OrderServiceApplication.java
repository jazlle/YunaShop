package com.yuna.yunashop.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Le scheduling fait tourner le relais de l'outbox et le timeout de saga. */
@SpringBootApplication
@EnableScheduling
public class OrderServiceApplication {

    /** Package des classes générées depuis les schémas Avro (src/main/avro). */
    private static final String AVRO_EVENTS_PACKAGE = "com.yuna.yunashop.events";

    static void main(String[] args) {
        // Depuis Avro 1.12, seules les classes de packages déclarés "de confiance" peuvent être (dé)sérialisées.
        // Doit être défini avant le premier chargement d'Avro.
        if (System.getProperty("org.apache.avro.SERIALIZABLE_PACKAGES") == null) {
            System.setProperty("org.apache.avro.SERIALIZABLE_PACKAGES", AVRO_EVENTS_PACKAGE);
        }
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
