package com.uday.rguktconnect.repository;

import com.uday.rguktconnect.entity.Job;
import com.uday.rguktconnect.entity.User;
import com.uday.rguktconnect.repository.user.UserRepository;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class JobRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public List<Job> findAllByOrderByCreatedAtDesc() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (j:Job)<-[:POSTED]-(u:User) " +
                    "RETURN j, u ORDER BY j.createdAt DESC"
                );
                List<Job> jobs = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Node jNode = record.get("j").asNode();
                    Node uNode = record.get("u").asNode();
                    User postedBy = userRepository.mapNodeToUser(uNode);
                    jobs.add(mapNodeToJob(jNode, postedBy));
                }
                return jobs;
            });
        }
    }

    public Optional<Job> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (j:Job {id: $id})<-[:POSTED]-(u:User) RETURN j, u",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Node jNode = record.get("j").asNode();
                    Node uNode = record.get("u").asNode();
                    User postedBy = userRepository.mapNodeToUser(uNode);
                    return Optional.of(mapNodeToJob(jNode, postedBy));
                }
                return Optional.empty();
            });
        }
    }

    public Job save(Job job) {
        if (job.getId() == null) {
            job.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        if (job.getCreatedAt() == null) {
            job.setCreatedAt(LocalDateTime.now());
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (j:Job {id: $id}) " +
                    "SET j.company = $company, " +
                    "    j.role = $role, " +
                    "    j.location = $location, " +
                    "    j.salary = $salary, " +
                    "    j.applyUrl = $applyUrl, " +
                    "    j.type = $type, " +
                    "    j.expiresAt = $expiresAt, " +
                    "    j.referralAvailable = $referralAvailable, " +
                    "    j.category = $category, " +
                    "    j.createdAt = $createdAt " +
                    "WITH j " +
                    "MATCH (u:User {id: $userId}) " +
                    "MERGE (u)-[:POSTED]->(j) " +
                    "RETURN j",
                    Values.parameters(
                        "id", job.getId(),
                        "company", job.getCompany() != null ? job.getCompany() : "",
                        "role", job.getRole() != null ? job.getRole() : "",
                        "location", job.getLocation() != null ? job.getLocation() : "",
                        "salary", job.getSalary() != null ? job.getSalary() : "",
                        "applyUrl", job.getApplyUrl() != null ? job.getApplyUrl() : "",
                        "type", job.getType() != null ? job.getType() : "",
                        "expiresAt", job.getExpiresAt() != null ? job.getExpiresAt().toString() : "",
                        "referralAvailable", job.isReferralAvailable(),
                        "category", job.getCategory() != null ? job.getCategory() : "",
                        "createdAt", job.getCreatedAt().toString(),
                        "userId", job.getPostedBy().getId()
                    )
                );
                return null;
            });
        }
        return job;
    }

    public void delete(Job job) {
        if (job == null || job.getId() == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (j:Job {id: $id}) DETACH DELETE j",
                    Values.parameters("id", job.getId())
                );
                return null;
            });
        }
    }

    public List<Job> findAll() {
        return findAllByOrderByCreatedAtDesc();
    }

    private Job mapNodeToJob(Node node, User postedBy) {
        Job job = new Job();
        job.setId(node.get("id").asLong());
        job.setCompany(node.get("company").asString());
        job.setRole(node.get("role").asString());
        job.setLocation(node.get("location").asString());
        job.setSalary(node.get("salary").asString());
        job.setApplyUrl(node.get("applyUrl").asString());
        job.setType(node.get("type").asString());
        job.setReferralAvailable(node.get("referralAvailable").asBoolean());
        job.setCategory(node.get("category").asString());
        job.setPostedBy(postedBy);

        if (node.containsKey("expiresAt") && !node.get("expiresAt").asString().isEmpty()) {
            job.setExpiresAt(LocalDate.parse(node.get("expiresAt").asString()));
        }
        if (node.containsKey("createdAt") && !node.get("createdAt").isNull()) {
            job.setCreatedAt(LocalDateTime.parse(node.get("createdAt").asString()));
        }
        return job;
    }
}
