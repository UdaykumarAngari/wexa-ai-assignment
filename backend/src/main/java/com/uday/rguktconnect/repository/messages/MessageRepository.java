package com.uday.rguktconnect.repository.messages;

import com.uday.rguktconnect.entity.ChatMessage;
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
public class MessageRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public List<ChatMessage> findChatHistory(Long userA, Long userB) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (sender:User)-[:SENT_MESSAGE]->(m:ChatMessage)-[:RECEIVED_BY]->(receiver:User) " +
                    "WHERE (sender.id = $userA AND receiver.id = $userB) OR (sender.id = $userB AND receiver.id = $userA) " +
                    "RETURN m, sender, receiver " +
                    "ORDER BY m.timestamp ASC",
                    Values.parameters("userA", userA, "userB", userB)
                );
                List<ChatMessage> history = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Node mNode = record.get("m").asNode();
                    Node sNode = record.get("sender").asNode();
                    Node rNode = record.get("receiver").asNode();
                    User sender = userRepository.mapNodeToUser(sNode);
                    User receiver = userRepository.mapNodeToUser(rNode);
                    history.add(mapNodeToMessage(mNode, sender, receiver));
                }
                return history;
            });
        }
    }

    public long countUnreadMessages(Long userId) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (m:ChatMessage)-[:RECEIVED_BY]->(u:User {id: $userId}) " +
                    "WHERE m.isRead = false " +
                    "RETURN count(m) AS count",
                    Values.parameters("userId", userId)
                );
                if (result.hasNext()) {
                    return result.next().get("count").asLong();
                }
                return 0L;
            });
        }
    }

    public List<Object[]> countUnreadGroupBySender(Long userId) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (sender:User)-[:SENT_MESSAGE]->(m:ChatMessage)-[:RECEIVED_BY]->(u:User {id: $userId}) " +
                    "WHERE m.isRead = false " +
                    "RETURN sender.id AS senderId, count(m) AS count",
                    Values.parameters("userId", userId)
                );
                List<Object[]> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Long senderId = record.get("senderId").asLong();
                    Long count = record.get("count").asLong();
                    list.add(new Object[]{senderId, count});
                }
                return list;
            });
        }
    }

    public void markThreadAsRead(Long senderId, Long receiverId) {
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (sender:User {id: $senderId})-[:SENT_MESSAGE]->(m:ChatMessage)-[:RECEIVED_BY]->(receiver:User {id: $receiverId}) " +
                    "WHERE m.isRead = false " +
                    "SET m.isRead = true",
                    Values.parameters("senderId", senderId, "receiverId", receiverId)
                );
                return null;
            });
        }
    }

    public ChatMessage save(ChatMessage message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        if (message.getTimestamp() == null) {
            message.setTimestamp(LocalDateTime.now());
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (m:ChatMessage {id: $id}) " +
                    "SET m.content = $content, " +
                    "    m.isRead = $isRead, " +
                    "    m.timestamp = $timestamp " +
                    "WITH m " +
                    "MATCH (sender:User {id: $senderId}) " +
                    "MERGE (sender)-[:SENT_MESSAGE]->(m) " +
                    "WITH m " +
                    "MATCH (receiver:User {id: $receiverId}) " +
                    "MERGE (m)-[:RECEIVED_BY]->(receiver) " +
                    "RETURN m",
                    Values.parameters(
                        "id", message.getId(),
                        "content", message.getContent() != null ? message.getContent() : "",
                        "isRead", message.isRead(),
                        "timestamp", message.getTimestamp().toString(),
                        "senderId", message.getSender().getId(),
                        "receiverId", message.getReceiver().getId()
                    )
                );
                return null;
            });
        }
        return message;
    }

    private ChatMessage mapNodeToMessage(Node node, User sender, User receiver) {
        ChatMessage msg = new ChatMessage();
        msg.setId(node.get("id").asLong());
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(node.get("content").asString());
        msg.setRead(node.get("isRead").asBoolean());
        if (node.containsKey("timestamp") && !node.get("timestamp").isNull()) {
            msg.setTimestamp(LocalDateTime.parse(node.get("timestamp").asString()));
        }
        return msg;
    }
}