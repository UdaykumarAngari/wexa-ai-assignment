package com.uday.rguktconnect.repository.notification;

import com.uday.rguktconnect.entity.Notification;
import com.uday.rguktconnect.entity.User;
import com.uday.rguktconnect.repository.user.UserRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class NotificationRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient) {
        if (recipient == null) return Collections.emptyList();
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (sender:User)-[:TRIGGERED_NOTIFICATION]->(n:Notification)-[:RECEIVED_NOTIFICATION]->(recipient:User {id: $recipientId}) " +
                    "RETURN n, sender, recipient " +
                    "ORDER BY n.createdAt DESC",
                    Values.parameters("recipientId", recipient.getId())
                );
                List<Notification> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = recordToNotification(result.next(), recipient);
                    if (record != null) {
                        list.add(record);
                    }
                }
                return list;
            });
        }
    }

    public long countByRecipientAndIsReadFalse(User recipient) {
        if (recipient == null) return 0L;
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (n:Notification)-[:RECEIVED_NOTIFICATION]->(u:User {id: $userId}) " +
                    "WHERE n.isRead = false " +
                    "RETURN count(n) AS count",
                    Values.parameters("userId", recipient.getId())
                );
                if (result.hasNext()) {
                    return result.next().get("count").asLong();
                }
                return 0L;
            });
        }
    }

    public void markAllAsRead(User recipient) {
        if (recipient == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (n:Notification)-[:RECEIVED_NOTIFICATION]->(u:User {id: $userId}) " +
                    "WHERE n.isRead = false " +
                    "SET n.isRead = true",
                    Values.parameters("userId", recipient.getId())
                );
                return null;
            });
        }
    }

    public void deleteByRelatedIdAndType(Long relatedId, String type) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (n:Notification {relatedId: $relatedId, type: $type}) " +
                    "DETACH DELETE n",
                    Values.parameters("relatedId", relatedId, "type", type)
                );
                return null;
            });
        }
    }

    public Notification save(Notification n) {
        if (n.getId() == null) {
            n.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        if (n.getCreatedAt() == null) {
            n.setCreatedAt(LocalDateTime.now());
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (n:Notification {id: $id}) " +
                    "SET n.type = $type, " +
                    "    n.relatedId = $relatedId, " +
                    "    n.isRead = $isRead, " +
                    "    n.createdAt = $createdAt " +
                    "WITH n " +
                    "MATCH (recipient:User {id: $recipientId}) " +
                    "MERGE (n)-[:RECEIVED_NOTIFICATION]->(recipient) " +
                    "WITH n " +
                    "MATCH (sender:User {id: $senderId}) " +
                    "MERGE (sender)-[:TRIGGERED_NOTIFICATION]->(n) " +
                    "RETURN n",
                    Values.parameters(
                        "id", n.getId(),
                        "type", n.getType() != null ? n.getType() : "",
                        "relatedId", n.getRelatedId() != null ? n.getRelatedId() : null,
                        "isRead", n.isRead(),
                        "createdAt", n.getCreatedAt().toString(),
                        "recipientId", n.getRecipient().getId(),
                        "senderId", n.getSender().getId()
                    )
                );
                return null;
            });
        }
        return n;
    }

    public Optional<Notification> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (sender:User)-[:TRIGGERED_NOTIFICATION]->(n:Notification {id: $id})-[:RECEIVED_NOTIFICATION]->(recipient:User) " +
                    "RETURN n, sender, recipient",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Node rNode = record.get("recipient").asNode();
                    User recipient = userRepository.mapNodeToUser(rNode);
                    return Optional.of(recordToNotification(record, recipient));
                }
                return Optional.empty();
            });
        }
    }

    private Notification recordToNotification(org.neo4j.driver.Record record, User recipient) {
        Node nNode = record.get("n").asNode();
        Node sNode = record.get("sender").asNode();
        User sender = userRepository.mapNodeToUser(sNode);

        return Notification.builder()
                .id(nNode.get("id").asLong())
                .recipient(recipient)
                .sender(sender)
                .type(nNode.get("type").asString())
                .relatedId(nNode.containsKey("relatedId") && !nNode.get("relatedId").isNull() ? nNode.get("relatedId").asLong() : null)
                .isRead(nNode.get("isRead").asBoolean())
                .createdAt(nNode.containsKey("createdAt") && !nNode.get("createdAt").isNull() ? LocalDateTime.parse(nNode.get("createdAt").asString()) : LocalDateTime.now())
                .build();
    }
}
