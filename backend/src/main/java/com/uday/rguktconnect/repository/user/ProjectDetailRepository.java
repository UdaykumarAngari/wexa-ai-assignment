package com.uday.rguktconnect.repository.user;

import com.uday.rguktconnect.entity.Project;
import com.uday.rguktconnect.entity.User;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ProjectDetailRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public List<Project> findByUser(User user) {
        if (user == null || user.getId() == null) {
            return Collections.emptyList();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $userId})-[:HAS_PROJECT]->(p:Project) RETURN p",
                    Values.parameters("userId", user.getId())
                );
                List<Project> list = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("p").asNode();
                    list.add(mapNodeToProject(node, user));
                }
                return list;
            });
        }
    }

    public Optional<Project> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User)-[:HAS_PROJECT]->(p:Project {id: $id}) RETURN p, u",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Node pNode = record.get("p").asNode();
                    Node uNode = record.get("u").asNode();
                    User user = userRepository.mapNodeToUser(uNode);
                    return Optional.of(mapNodeToProject(pNode, user));
                }
                return Optional.empty();
            });
        }
    }

    public Project save(Project project) {
        if (project.getId() == null) {
            project.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (p:Project {id: $id}) " +
                    "SET p.title = $title, " +
                    "    p.description = $description, " +
                    "    p.projectUrl = $projectUrl, " +
                    "    p.repoUrl = $repoUrl " +
                    "WITH p " +
                    "MATCH (u:User {id: $userId}) " +
                    "MERGE (u)-[:HAS_PROJECT]->(p) " +
                    "RETURN p",
                    Values.parameters(
                        "id", project.getId(),
                        "title", project.getTitle() != null ? project.getTitle() : "",
                        "description", project.getDescription() != null ? project.getDescription() : "",
                        "projectUrl", project.getProjectUrl() != null ? project.getProjectUrl() : "",
                        "repoUrl", project.getRepoUrl() != null ? project.getRepoUrl() : "",
                        "userId", project.getUser().getId()
                    )
                );
                return null;
            });
        }
        return project;
    }

    public void delete(Project project) {
        if (project == null || project.getId() == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (p:Project {id: $id}) DETACH DELETE p",
                    Values.parameters("id", project.getId())
                );
                return null;
            });
        }
    }

    private Project mapNodeToProject(Node node, User user) {
        Project project = new Project();
        project.setId(node.get("id").asLong());
        project.setUser(user);
        project.setTitle(node.get("title").asString());
        project.setDescription(node.get("description").asString());
        project.setProjectUrl(node.get("projectUrl").asString());
        project.setRepoUrl(node.get("repoUrl").asString());
        return project;
    }
}