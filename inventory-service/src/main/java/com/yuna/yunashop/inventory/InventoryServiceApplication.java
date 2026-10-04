package com.yuna.yunashop.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InventoryServiceApplication {

    /** Package des classes générées depuis les schémas Avro (src/main/avro). */
    private static final String AVRO_EVENTS_PACKAGE = "com.yuna.yunashop.events";

    static void main(String[] args) {
        // Depuis Avro 1.12, seules les classes de packages déclarés "de confiance" peuvent être (dé)sérialisées.
        // Doit être défini avant le premier chargement d'Avro.
        if (System.getProperty("org.apache.avro.SERIALIZABLE_PACKAGES") == null) {
            System.setProperty("org.apache.avro.SERIALIZABLE_PACKAGES", AVRO_EVENTS_PACKAGE);
        }
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
