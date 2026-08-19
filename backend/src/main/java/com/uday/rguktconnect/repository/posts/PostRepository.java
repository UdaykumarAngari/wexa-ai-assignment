package com.uday.rguktconnect.repository.posts;

import com.uday.rguktconnect.entity.Post;
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
public class PostRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    public List<Post> findAllByOrderByCreatedAtDesc() {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Post)<-[:AUTHORED]-(u:User) " +
                    "RETURN p, u ORDER BY p.createdAt DESC"
                );
                List<Post> posts = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Node pNode = record.get("p").asNode();
                    Node uNode = record.get("u").asNode();
                    User author = userRepository.mapNodeToUser(uNode);
                    posts.add(mapNodeToPost(pNode, author));
                }
                return posts;
            });
        }
    }

    public Optional<Post> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Post {id: $id})<-[:AUTHORED]-(u:User) " +
                    "RETURN p, u",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Node pNode = record.get("p").asNode();
                    Node uNode = record.get("u").asNode();
                    User author = userRepository.mapNodeToUser(uNode);
                    return Optional.of(mapNodeToPost(pNode, author));
                }
                return Optional.empty();
            });
        }
    }

    public Post save(Post post) {
        if (post.getId() == null) {
            post.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(LocalDateTime.now());
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (p:Post {id: $id}) " +
                    "SET p.type = $type, " +
                    "    p.content = $content, " +
                    "    p.codeSnippet = $codeSnippet, " +
                    "    p.company = $company, " +
                    "    p.role = $role, " +
                    "    p.mediaUrl = $mediaUrl, " +
                    "    p.createdAt = $createdAt " +
                    "WITH p " +
                    "MATCH (u:User {id: $authorId}) " +
                    "MERGE (u)-[:AUTHORED]->(p) " +
                    "RETURN p",
                    Values.parameters(
                        "id", post.getId(),
                        "type", post.getType() != null ? post.getType() : "text",
                        "content", post.getContent() != null ? post.getContent() : "",
                        "codeSnippet", post.getCodeSnippet() != null ? post.getCodeSnippet() : "",
                        "company", post.getCompany() != null ? post.getCompany() : "",
                        "role", post.getRole() != null ? post.getRole() : "",
                        "mediaUrl", post.getMediaUrl() != null ? post.getMediaUrl() : "",
                        "createdAt", post.getCreatedAt().toString(),
                        "authorId", post.getAuthor().getId()
                    )
                );

                // Update liked relationship
                tx.run("MATCH (p:Post {id: $id})<-[r:LIKED]-(:User) DELETE r", Values.parameters("id", post.getId()));
                if (post.getLikedBy() != null) {
                    for (User user : post.getLikedBy()) {
                        tx.run(
                            "MATCH (p:Post {id: $postId}), (u:User {id: $userId}) " +
                            "MERGE (u)-[:LIKED]->(p)",
                            Values.parameters("postId", post.getId(), "userId", user.getId())
                        );
                    }
                }
                return null;
            });
        }
        return post;
    }

    public List<Post> findAll() {
        return findAllByOrderByCreatedAtDesc();
    }

    public void delete(Post post) {
        if (post == null || post.getId() == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (p:Post {id: $id}) DETACH DELETE p",
                    Values.parameters("id", post.getId())
                );
                return null;
            });
        }
    }

    private Post mapNodeToPost(Node node, User author) {
        Post post = new Post();
        post.setId(node.get("id").asLong());
        post.setAuthor(author);
        post.setType(node.get("type").asString());
        post.setContent(node.get("content").asString());
        post.setCodeSnippet(node.get("codeSnippet").asString());
        post.setCompany(node.get("company").asString());
        post.setRole(node.get("role").asString());
        post.setMediaUrl(node.get("mediaUrl").asString());
        if (node.containsKey("createdAt") && !node.get("createdAt").isNull()) {
            post.setCreatedAt(LocalDateTime.parse(node.get("createdAt").asString()));
        }

        // Fetch likedBy
        Set<User> likedBy = new HashSet<>();
        try (Session session = driver.session()) {
            session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Post {id: $postId})<-[:LIKED]-(u:User) RETURN u",
                    Values.parameters("postId", post.getId())
                );
                while (result.hasNext()) {
                    likedBy.add(userRepository.mapNodeToUser(result.next().get("u").asNode()));
                }
                return null;
            });
        }
        post.setLikedBy(likedBy);

        return post;
    }
}
