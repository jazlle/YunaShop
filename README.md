# YunaShop – microservices Spring Boot

Trois microservices Spring Boot 4 (Java 27) qui coordonnent une commande via une **saga chorégraphiée sur Kafka**,
avec des événements **Avro** enregistrés dans un **Confluent Schema Registry**.
Chaque service possède sa propre base PostgreSQL, versionnée avec **Liquibase**.

| Module              | Port | Base          | Rôle                                                            |
|---------------------|------|---------------|-----------------------------------------------------------------|
| `order-service`     | 8081 | `orderdb`     | Crée les commandes et suit leur statut                          |
| `inventory-service` | 8082 | `inventorydb` | Réserve le stock, calcule le montant, libère le stock si besoin |
| `payment-service`   | 8083 | `paymentdb`   | Débite le compte client                                         |

## Déroulement de la saga

```
POST /api/orders ─► order.created ─► inventory ─┬─► inventory.reserved ─► payment ─┬─► payment.completed ─► order CONFIRMED
   (PENDING)                                     │                                  └─► payment.failed ────► order CANCELLED
   │                                             │                                                          + inventory libère le stock
   │                                             └─► inventory.failed ─► order REJECTED
```

Les consommateurs sont idempotents (une réservation / un paiement par `orderId`) : un message rejoué republie
le même résultat sans décrémenter le stock ni débiter deux fois.

## Services autonomes (prêts à être extraits du monorepo)

Aucun code n'est partagé entre les services :

- chaque `pom.xml` hérite directement de `spring-boot-starter-parent` ; le `pom.xml` racine est un simple agrégateur ;
- chaque service embarque **sa copie** des contrats d'événements dont il a besoin (`src/main/avro/*.avsc`)
  et sa propre classe `Topics`.

Pour extraire un service dans son propre dépôt : copier son dossier, plus `mvnw`, `mvnw.cmd` et `.mvn/`.

### Contrats Avro

| Événement (topic)                            | Producteur | Consommateurs     |
|----------------------------------------------|------------|-------------------|
| `OrderCreatedEvent` (`order.created`)        | order      | inventory         |
| `InventoryReservedEvent` (`inventory.reserved`) | inventory | payment         |
| `InventoryFailedEvent` (`inventory.failed`)  | inventory  | order             |
| `PaymentCompletedEvent` (`payment.completed`)| payment    | order             |
| `PaymentFailedEvent` (`payment.failed`)      | payment    | order, inventory  |

Les classes Java sont générées au build par `avro-maven-plugin` (`target/generated-sources/avro`).

Règles à respecter sur les copies dupliquées :

- **garder le même `namespace` + `name`** (`com.yuna.yunashop.events.*`) : le désérialiseur Confluent retrouve
  la classe Java à partir du nom complet du schéma du producteur ;
- **faire évoluer les schémas de façon compatible** (ajout de champ avec valeur par défaut, etc.) :
  le registry est configuré en compatibilité `BACKWARD` et refusera un schéma incompatible.
  C'est le registry, et non plus un module partagé, qui garantit la cohérence entre services.

Schémas enregistrés (sujets `<topic>-value`) : `curl localhost:8085/subjects`.

### Liquibase

Chaque service a son changelog dans `src/main/resources/db/changelog/` (`db.changelog-master.yaml` + `changes/`).
Hibernate est en `ddl-auto: validate` : toute évolution du schéma passe par un nouveau changeset (ne jamais modifier
un changeset déjà appliqué). Les données de démo sont chargées par les changesets `003-seed-*`.

## Démarrage

```bash
docker compose up -d                      # Kafka (KRaft) + Schema Registry + PostgreSQL
./mvnw install -DskipTests

./mvnw -pl order-service spring-boot:run
./mvnw -pl inventory-service spring-boot:run
./mvnw -pl payment-service spring-boot:run
```

| Infra           | Adresse côté hôte       |
|-----------------|-------------------------|
| Kafka           | `localhost:9092`        |
| Schema Registry | `http://localhost:8085` |
| PostgreSQL      | `localhost:5432` (`yuna` / `yuna`) |

Variables d'environnement surchargeables : `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`,
`KAFKA_BOOTSTRAP_SERVERS`, `SCHEMA_REGISTRY_URL`.

## Données de démo

- Produits : `KB-001` (89,90 €, stock 50), `MS-001` (29,90 €, stock 100), `SC-001` (249 €, stock 5)
- Comptes : `alice` (1000 €), `bob` (50 €)

## Scénarios

```bash
# Succès → CONFIRMED
curl -X POST localhost:8081/api/orders -H "Content-Type: application/json" \
     -d '{"customerId":"alice","productId":"KB-001","quantity":2}'

# Stock insuffisant → REJECTED
curl -X POST localhost:8081/api/orders -H "Content-Type: application/json" \
     -d '{"customerId":"alice","productId":"SC-001","quantity":10}'

# Solde insuffisant → CANCELLED (et le stock est remis à disposition)
curl -X POST localhost:8081/api/orders -H "Content-Type: application/json" \
     -d '{"customerId":"bob","productId":"SC-001","quantity":1}'

curl localhost:8081/api/orders              # statut des commandes
curl localhost:8082/api/products            # stock
curl localhost:8083/api/payments            # paiements
curl localhost:8083/api/accounts/alice      # solde
```

## Pistes d'évolution

- **Transactional Outbox** : l'écriture en base et l'envoi Kafka ne sont pas atomiques aujourd'hui.
- **Dead Letter Topic** (`DefaultErrorHandler` + `DeadLetterPublishingRecoverer`) pour les messages en échec.
- En production : `auto.register.schemas=false` côté producteurs, schémas enregistrés par la CI
  (`kafka-schema-registry-maven-plugin` : `test-compatibility` puis `register`).
- API Gateway, tests d'intégration avec Testcontainers, traçage distribué (Micrometer Tracing).
