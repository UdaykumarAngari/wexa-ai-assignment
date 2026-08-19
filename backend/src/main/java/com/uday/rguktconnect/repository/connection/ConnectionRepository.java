package com.uday.rguktconnect.repository.connection;

import com.uday.rguktconnect.entity.Connection;
import com.uday.rguktconnect.entity.User;
import com.uday.rguktconnect.repository.user.UserRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class ConnectionRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public Optional<Connection> findBySenderAndReceiver(User sender, User receiver) {
        if (sender == null || receiver == null) return Optional.empty();
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (sender:User {id: $senderId})-[r:CONNECTED]->(receiver:User {id: $receiverId}) " +
                    "RETURN r, sender, receiver",
                    Values.parameters("senderId", sender.getId(), "receiverId", receiver.getId())
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Relationship r = record.get("r").asRelationship();
                    return Optional.of(mapRelationToConnection(r, sender, receiver));
                }
                return Optional.empty();
            });
        }
    }

    public List<Connection> findByReceiverAndStatus(User receiver, String status) {
        if (receiver == null) return Collections.emptyList();
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (sender:User)-[r:CONNECTED {status: $status}]->(receiver:User {id: $receiverId}) " +
                    "RETURN r, sender",
                    Values.parameters("receiverId", receiver.getId(), "status", status)
                );
                List<Connection> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Relationship r = record.get("r").asRelationship();
                    User sender = userRepository.mapNodeToUser(record.get("sender").asNode());
                    list.add(mapRelationToConnection(r, sender, receiver));
                }
                return list;
            });
        }
    }

    public Optional<Connection> findConnectionBetweenUsers(User userA, User userB) {
        if (userA == null || userB == null) return Optional.empty();
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (uA:User {id: $userA})-[r:CONNECTED]-(uB:User {id: $userB}) " +
                    "RETURN r, startNode(r) AS senderNode, endNode(r) AS receiverNode",
                    Values.parameters("userA", userA.getId(), "userB", userB.getId())
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Relationship r = record.get("r").asRelationship();
                    Node sNode = record.get("senderNode").asNode();
                    Node rNode = record.get("receiverNode").asNode();
                    User sender = userRepository.mapNodeToUser(sNode);
                    User receiver = userRepository.mapNodeToUser(rNode);
                    return Optional.of(mapRelationToConnection(r, sender, receiver));
                }
                return Optional.empty();
            });
        }
    }

    public List<Long> findConnectedUserIds(Long userId) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $userId})-[r:CONNECTED {status: 'ACCEPTED'}]-(other:User) " +
                    "RETURN other.id AS id",
                    Values.parameters("userId", userId)
                );
                List<Long> list = new ArrayList<>();
                while (result.hasNext()) {
                    list.add(result.next().get("id").asLong());
                }
                return list;
            });
        }
    }

    public List<User> findConnectedUsers(Long userId) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $userId})-[r:CONNECTED {status: 'ACCEPTED'}]-(other:User) " +
                    "RETURN other",
                    Values.parameters("userId", userId)
                );
                List<User> list = new ArrayList<>();
                while (result.hasNext()) {
                    list.add(userRepository.mapNodeToUser(result.next().get("other").asNode()));
                }
                return list;
            });
        }
    }

    public boolean areUsersConnected(Long userAId, Long userBId) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (uA:User {id: $userAId})-[r:CONNECTED {status: 'ACCEPTED'}]-(uB:User {id: $userBId}) " +
                    "RETURN count(r) > 0 AS connected",
                    Values.parameters("userAId", userAId, "userBId", userBId)
                );
                if (result.hasNext()) {
                    return result.next().get("connected").asBoolean();
                }
                return false;
            });
        }
    }

    public Connection save(Connection connection) {
        if (connection.getId() == null) {
            connection.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        if (connection.getCreatedAt() == null) {
            connection.setCreatedAt(LocalDateTime.now());
        }
        connection.setUpdatedAt(LocalDateTime.now());

        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (sender:User {id: $senderId}), (receiver:User {id: $receiverId}) " +
                    "MERGE (sender)-[r:CONNECTED]->(receiver) " +
                    "SET r.id = $id, " +
                    "    r.status = $status, " +
                    "    r.createdAt = $createdAt, " +
                    "    r.updatedAt = $updatedAt " +
                    "RETURN r",
                    Values.parameters(
                        "senderId", connection.getSender().getId(),
                        "receiverId", connection.getReceiver().getId(),
                        "id", connection.getId(),
                        "status", connection.getStatus(),
                        "createdAt", connection.getCreatedAt().toString(),
                        "updatedAt", connection.getUpdatedAt().toString()
                    )
                );
                return null;
            });
        }
        return connection;
    }

    public Optional<Connection> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (sender:User)-[r:CONNECTED {id: $id}]->(receiver:User) " +
                    "RETURN r, sender, receiver",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Relationship r = record.get("r").asRelationship();
                    User sender = userRepository.mapNodeToUser(record.get("sender").asNode());
                    User receiver = userRepository.mapNodeToUser(record.get("receiver").asNode());
                    return Optional.of(mapRelationToConnection(r, sender, receiver));
                }
                return Optional.empty();
            });
        }
    }

    public List<Connection> findAll() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (sender:User)-[r:CONNECTED]->(receiver:User) " +
                    "RETURN r, sender, receiver"
                );
                List<Connection> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Relationship r = record.get("r").asRelationship();
                    User sender = userRepository.mapNodeToUser(record.get("sender").asNode());
                    User receiver = userRepository.mapNodeToUser(record.get("receiver").asNode());
                    list.add(mapRelationToConnection(r, sender, receiver));
                }
                return list;
            });
        }
    }

    public void delete(Connection connection) {
        if (connection == null || connection.getSender() == null || connection.getReceiver() == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (sender:User {id: $senderId})-[r:CONNECTED]->(receiver:User {id: $receiverId}) " +
                    "DELETE r",
                    Values.parameters(
                        "senderId", connection.getSender().getId(),
                        "receiverId", connection.getReceiver().getId()
                    )
                );
                return null;
            });
        }
    }

    private Connection mapRelationToConnection(Relationship r, User sender, User receiver) {
        Connection conn = new Connection();
        conn.setId(r.get("id").asLong());
        conn.setSender(sender);
        conn.setReceiver(receiver);
        conn.setStatus(r.get("status").asString());
        if (r.containsKey("createdAt") && !r.get("createdAt").isNull()) {
            conn.setCreatedAt(LocalDateTime.parse(r.get("createdAt").asString()));
        }
        if (r.containsKey("updatedAt") && !r.get("updatedAt").isNull()) {
            conn.setUpdatedAt(LocalDateTime.parse(r.get("updatedAt").asString()));
        }
        return conn;
    }

    public List<Map<String, Object>> findRecommendedConnections(Long userId) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $userId})-[:CONNECTED {status: 'ACCEPTED'}]-(friend:User)-[:CONNECTED {status: 'ACCEPTED'}]-(fof:User) " +
                    "WHERE NOT (u)-[:CONNECTED]-(fof) AND u.id <> fof.id " +
                    "OPTIONAL MATCH (fof)-[:HAS_DETAILS]->(d:UserDetails) " +
                    "RETURN fof, d.profilePhoto AS profilePhoto, count(friend) AS mutualCount, collect(friend.name) AS mutualFriends " +
                    "ORDER BY mutualCount DESC LIMIT 6",
                    Values.parameters("userId", userId)
                );
                List<Map<String, Object>> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Node fofNode = record.get("fof").asNode();
                    long mutualCount = record.get("mutualCount").asLong();
                    String profilePhoto = record.get("profilePhoto").isNull() ? null : record.get("profilePhoto").asString();
                    List<Object> mutualFriends = record.get("mutualFriends").asList();

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", fofNode.get("id").asLong());
                    map.put("idNumber", fofNode.get("idNumber").asString());
                    map.put("name", fofNode.get("name").asString());
                    map.put("universityEmail", fofNode.get("universityEmail").asString());
                    map.put("role", fofNode.get("role").asString());
                    map.put("profilePhoto", profilePhoto);
                    map.put("mutualCount", mutualCount);
                    map.put("mutualFriends", mutualFriends);
                    list.add(map);
                }
                return list;
            });
        }
    }
}