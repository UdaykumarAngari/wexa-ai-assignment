package com.uday.rguktconnect.repository.user;

import com.uday.rguktconnect.entity.User;
import com.uday.rguktconnect.entity.UserDetails;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class UserDetailsRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public Optional<UserDetails> findByUser(User user) {
        if (user == null || user.getId() == null) {
            return Optional.empty();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $userId})-[:HAS_DETAILS]->(d:UserDetails) RETURN d",
                    Values.parameters("userId", user.getId())
                );
                if (result.hasNext()) {
                    Node node = result.next().get("d").asNode();
                    return Optional.of(mapNodeToUserDetails(node, user));
                }
                return Optional.empty();
            });
        }
    }

    public UserDetails save(UserDetails details) {
        if (details.getId() == null) {
            details.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        if (details.getUpdatedAt() == null) {
            details.setUpdatedAt(LocalDateTime.now());
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (d:UserDetails {id: $id}) " +
                    "SET d.mobileNumber = $mobileNumber, " +
                    "    d.personalEmail = $personalEmail, " +
                    "    d.branch = $branch, " +
                    "    d.batch = $batch, " +
                    "    d.profilePhoto = $profilePhoto, " +
                    "    d.description = $description, " +
                    "    d.githubUrl = $githubUrl, " +
                    "    d.linkedinUrl = $linkedinUrl, " +
                    "    d.mentoredStudentsCount = $mentoredStudentsCount, " +
                    "    d.updatedAt = $updatedAt " +
                    "WITH d " +
                    "MATCH (u:User {id: $userId}) " +
                    "MERGE (u)-[:HAS_DETAILS]->(d) " +
                    "RETURN d",
                    Values.parameters(
                        "id", details.getId(),
                        "mobileNumber", details.getMobileNumber() != null ? details.getMobileNumber() : "",
                        "personalEmail", details.getPersonalEmail() != null ? details.getPersonalEmail() : "",
                        "branch", details.getBranch() != null ? details.getBranch() : "",
                        "batch", details.getBatch() != null ? details.getBatch() : "",
                        "profilePhoto", details.getProfilePhoto() != null ? details.getProfilePhoto() : "",
                        "description", details.getDescription() != null ? details.getDescription() : "",
                        "githubUrl", details.getGithubUrl() != null ? details.getGithubUrl() : "",
                        "linkedinUrl", details.getLinkedinUrl() != null ? details.getLinkedinUrl() : "",
                        "mentoredStudentsCount", details.getMentoredStudentsCount() != null ? details.getMentoredStudentsCount() : 0,
                        "updatedAt", details.getUpdatedAt().toString(),
                        "userId", details.getUser().getId()
                    )
                );
                return null;
            });
        }
        return details;
    }

    public List<UserDetails> findAll() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("MATCH (u:User)-[:HAS_DETAILS]->(d:UserDetails) RETURN d, u");
                List<UserDetails> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Node dNode = record.get("d").asNode();
                    Node uNode = record.get("u").asNode();
                    User user = userRepository.mapNodeToUser(uNode);
                    list.add(mapNodeToUserDetails(dNode, user));
                }
                return list;
            });
        }
    }

    private UserDetails mapNodeToUserDetails(Node node, User user) {
        UserDetails details = new UserDetails();
        details.setId(node.get("id").asLong());
        details.setUser(user);
        details.setMobileNumber(node.get("mobileNumber").asString());
        details.setPersonalEmail(node.get("personalEmail").asString());
        details.setBranch(node.get("branch").asString());
        details.setBatch(node.get("batch").asString());
        details.setProfilePhoto(node.get("profilePhoto").asString());
        details.setDescription(node.get("description").asString());
        details.setGithubUrl(node.get("githubUrl").asString());
        details.setLinkedinUrl(node.get("linkedinUrl").asString());
        details.setMentoredStudentsCount(node.get("mentoredStudentsCount").asInt());
        if (node.containsKey("updatedAt") && !node.get("updatedAt").isNull()) {
            details.setUpdatedAt(LocalDateTime.parse(node.get("updatedAt").asString()));
        }
        return details;
    }
}