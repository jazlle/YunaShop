package com.yuna.yunashop.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

interface OutboxRepository extends JpaRepository<OutboxMessage, Long> {

    /**
     * Verrouille les plus anciens messages. SKIP LOCKED : plusieurs instances du relais peuvent
     * tourner en parallèle sans publier deux fois le même lot.
     */
    @Query(value = "SELECT * FROM outbox ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxMessage> lockOldest(int limit);

    /**
     * Si le relais est en train de publier l'un de ces messages, attend la fin de sa transaction :
     * le message est alors déjà supprimé et n'est pas compté.
     */
    @Modifying
    @Query("DELETE FROM OutboxMessage m WHERE m.aggregateId = :aggregateId AND m.topic = :topic")
    int deleteByAggregateIdAndTopic(UUID aggregateId, String topic);
}
