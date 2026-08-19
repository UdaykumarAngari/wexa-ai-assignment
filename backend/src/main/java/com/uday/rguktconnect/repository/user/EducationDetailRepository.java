package com.uday.rguktconnect.repository.user;

import com.uday.rguktconnect.entity.EducationDetail;
import com.uday.rguktconnect.entity.User;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class EducationDetailRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public List<EducationDetail> findByUser(User user) {
        if (user == null || user.getId() == null) {
            return Collections.emptyList();
        }
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User {id: $userId})-[:HAS_EDUCATION]->(e:EducationDetail) RETURN e",
                    Values.parameters("userId", user.getId())
                );
                List<EducationDetail> list = new ArrayList<>();
                while (result.hasNext()) {
                    Node node = result.next().get("e").asNode();
                    list.add(mapNodeToEducation(node, user));
                }
                return list;
            });
        }
    }

    public Optional<EducationDetail> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (u:User)-[:HAS_EDUCATION]->(e:EducationDetail {id: $id}) RETURN e, u",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Node eNode = record.get("e").asNode();
                    Node uNode = record.get("u").asNode();
                    User user = userRepository.mapNodeToUser(uNode);
                    return Optional.of(mapNodeToEducation(eNode, user));
                }
                return Optional.empty();
            });
        }
    }

    public EducationDetail save(EducationDetail education) {
        if (education.getId() == null) {
            education.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (e:EducationDetail {id: $id}) " +
                    "SET e.institutionName = $institutionName, " +
                    "    e.degree = $degree, " +
                    "    e.fieldOfStudy = $fieldOfStudy, " +
                    "    e.startYear = $startYear, " +
                    "    e.endYear = $endYear, " +
                    "    e.grade = $grade " +
                    "WITH e " +
                    "MATCH (u:User {id: $userId}) " +
                    "MERGE (u)-[:HAS_EDUCATION]->(e) " +
                    "RETURN e",
                    Values.parameters(
                        "id", education.getId(),
                        "institutionName", education.getInstitutionName() != null ? education.getInstitutionName() : "",
                        "degree", education.getDegree() != null ? education.getDegree() : "",
                        "fieldOfStudy", education.getFieldOfStudy() != null ? education.getFieldOfStudy() : "",
                        "startYear", education.getStartYear() != null ? education.getStartYear() : "",
                        "endYear", education.getEndYear() != null ? education.getEndYear() : "",
                        "grade", education.getGrade() != null ? education.getGrade() : "",
                        "userId", education.getUser().getId()
                    )
                );
                return null;
            });
        }
        return education;
    }

    public void delete(EducationDetail education) {
        if (education == null || education.getId() == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (e:EducationDetail {id: $id}) DETACH DELETE e",
                    Values.parameters("id", education.getId())
                );
                return null;
            });
        }
    }

    private EducationDetail mapNodeToEducation(Node node, User user) {
        EducationDetail edu = new EducationDetail();
        edu.setId(node.get("id").asLong());
        edu.setUser(user);
        edu.setInstitutionName(node.get("institutionName").asString());
        edu.setDegree(node.get("degree").asString());
        edu.setFieldOfStudy(node.get("fieldOfStudy").asString());
        edu.setStartYear(node.get("startYear").asString());
        edu.setEndYear(node.get("endYear").asString());
        edu.setGrade(node.get("grade").asString());
        return edu;
    }
}