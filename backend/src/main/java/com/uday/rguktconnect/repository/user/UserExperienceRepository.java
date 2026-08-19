package com.uday.rguktconnect.repository.user;

import com.uday.rguktconnect.entity.User;
import com.uday.rguktconnect.entity.UserExperiences;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.*;

@Repository
public class UserExperienceRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public List<UserExperiences> findByUser(User user) {
        if (user == null || user.getId() == null) {
            return Collections.emptyList();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $userId})-[:HAS_EXPERIENCE]->(e:UserExperience) RETURN e ORDER BY e.startDate DESC",
                    Values.parameters("userId", user.getId())
                );
                List<UserExperiences> list = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("e").asNode();
                    list.add(mapNodeToExperience(node, user));
                }
                return list;
            });
        }
    }

    public Optional<UserExperiences> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User)-[:HAS_EXPERIENCE]->(e:UserExperience {id: $id}) RETURN e, u",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Node eNode = record.get("e").asNode();
                    Node uNode = record.get("u").asNode();
                    User user = userRepository.mapNodeToUser(uNode);
                    return Optional.of(mapNodeToExperience(eNode, user));
                }
                return Optional.empty();
            });
        }
    }

    public UserExperiences save(UserExperiences experience) {
        if (experience.getId() == null) {
            experience.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (e:UserExperience {id: $id}) " +
                    "SET e.title = $title, " +
                    "    e.companyName = $companyName, " +
                    "    e.location = $location, " +
                    "    e.employmentType = $employmentType, " +
                    "    e.locationType = $locationType, " +
                    "    e.startDate = $startDate, " +
                    "    e.endDate = $endDate, " +
                    "    e.isCurrentRole = $isCurrentRole, " +
                    "    e.description = $description " +
                    "WITH e " +
                    "MATCH (u:User {id: $userId}) " +
                    "MERGE (u)-[:HAS_EXPERIENCE]->(e) " +
                    "RETURN e",
                    Values.parameters(
                        "id", experience.getId(),
                        "title", experience.getTitle() != null ? experience.getTitle() : "",
                        "companyName", experience.getCompanyName() != null ? experience.getCompanyName() : "",
                        "location", experience.getLocation() != null ? experience.getLocation() : "",
                        "employmentType", experience.getEmploymentType() != null ? experience.getEmploymentType() : "",
                        "locationType", experience.getLocationType() != null ? experience.getLocationType() : "",
                        "startDate", experience.getStartDate() != null ? experience.getStartDate().toString() : "",
                        "endDate", experience.getEndDate() != null ? experience.getEndDate().toString() : "",
                        "isCurrentRole", experience.isCurrentRole(),
                        "description", experience.getDescription() != null ? experience.getDescription() : "",
                        "userId", experience.getUser().getId()
                    )
                );
                return null;
            });
        }
        return experience;
    }

    public void delete(UserExperiences experience) {
        if (experience == null || experience.getId() == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (e:UserExperience {id: $id}) DETACH DELETE e",
                    Values.parameters("id", experience.getId())
                );
                return null;
            });
        }
    }

    public List<UserExperiences> findAll() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run("MATCH (u:User)-[:HAS_EXPERIENCE]->(e:UserExperience) RETURN e, u");
                List<UserExperiences> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Node eNode = record.get("e").asNode();
                    Node uNode = record.get("u").asNode();
                    User user = userRepository.mapNodeToUser(uNode);
                    list.add(mapNodeToExperience(eNode, user));
                }
                return list;
            });
        }
    }

    private UserExperiences mapNodeToExperience(Node node, User user) {
        UserExperiences experience = new UserExperiences();
        experience.setId(node.get("id").asLong());
        experience.setUser(user);
        experience.setTitle(node.get("title").asString());
        experience.setCompanyName(node.get("companyName").asString());
        experience.setLocation(node.get("location").asString());
        experience.setEmploymentType(node.get("employmentType").asString());
        experience.setLocationType(node.get("locationType").asString());
        experience.setCurrentRole(node.get("isCurrentRole").asBoolean());
        experience.setDescription(node.get("description").asString());

        if (node.containsKey("startDate") && !node.get("startDate").asString().isEmpty()) {
            experience.setStartDate(LocalDate.parse(node.get("startDate").asString()));
        }
        if (node.containsKey("endDate") && !node.get("endDate").asString().isEmpty()) {
            experience.setEndDate(LocalDate.parse(node.get("endDate").asString()));
        }
        return experience;
    }
}