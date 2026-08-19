package com.uday.rguktconnect.repository.user;

import com.uday.rguktconnect.entity.User;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class UserRepository {

    @Autowired
    private Driver driver;

    public Optional<User> findByUniversityEmail(String email) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User) WHERE toLower(u.universityEmail) = toLower($email) RETURN u",
                    Values.parameters("email", email)
                );
                if (result.hasNext()) {
                    Node node = result.next().get("u").asNode();
                    return Optional.of(mapNodeToUser(node));
                }
                return Optional.empty();
            });
        }
    }

    public boolean existsByIdNumberOrUniversityEmail(String idNumber, String email) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User) WHERE toLower(u.idNumber) = toLower($idNumber) OR toLower(u.universityEmail) = toLower($email) RETURN count(u) > 0 AS exists",
                    Values.parameters("idNumber", idNumber, "email", email)
                );
                if (result.hasNext()) {
                    return result.next().get("exists").asBoolean();
                }
                return false;
            });
        }
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (u:User {id: $id}) " +
                    "SET u.idNumber = $idNumber, " +
                    "    u.name = $name, " +
                    "    u.universityEmail = $universityEmail, " +
                    "    u.password = $password, " +
                    "    u.role = $role, " +
                    "    u.createdAt = $createdAt " +
                    "RETURN u",
                    Values.parameters(
                        "id", user.getId(),
                        "idNumber", user.getIdNumber(),
                        "name", user.getName(),
                        "universityEmail", user.getUniversityEmail(),
                        "password", user.getPassword(),
                        "role", user.getRole(),
                        "createdAt", user.getCreatedAt().toString()
                    )
                );
                return null;
            });
        }
        return user;
    }

    public Optional<User> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $id}) RETURN u",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    Node node = result.next().get("u").asNode();
                    return Optional.of(mapNodeToUser(node));
                }
                return Optional.empty();
            });
        }
    }

    public List<User> findAll() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("MATCH (u:User) RETURN u");
                List<User> users = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("u").asNode();
                    users.add(mapNodeToUser(node));
                }
                return users;
            });
        }
    }

    public boolean existsById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $id}) RETURN count(u) > 0 AS exists",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    return result.next().get("exists").asBoolean();
                }
                return false;
            });
        }
    }

    public User mapNodeToUser(Node node) {
        User user = new User();
        user.setId(node.get("id").asLong());
        user.setIdNumber(node.get("idNumber").asString());
        user.setName(node.get("name").asString());
        user.setUniversityEmail(node.get("universityEmail").asString());
        user.setPassword(node.get("password").asString());
        user.setRole(node.get("role").asString());
        if (node.containsKey("createdAt") && !node.get("createdAt").isNull()) {
            user.setCreatedAt(LocalDateTime.parse(node.get("createdAt").asString()));
        }
        return user;
    }
}