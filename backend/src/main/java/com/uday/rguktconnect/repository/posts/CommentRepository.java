package com.uday.rguktconnect.repository.posts;

import com.uday.rguktconnect.entity.Comment;
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
public class CommentRepository {

    @Autowired
    private Driver driver;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    public List<Comment> findByPostAndParentCommentIsNullOrderByCreatedAtAsc(Post post) {
        if (post == null || post.getId() == null) return Collections.emptyList();
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Post {id: $postId})-[:HAS_COMMENT]->(c:Comment) " +
                    "WHERE NOT (:Comment)-[:HAS_REPLY]->(c) " +
                    "MATCH (u:User)-[:COMMENTED]->(c) " +
                    "RETURN c, u ORDER BY c.createdAt ASC",
                    Values.parameters("postId", post.getId())
                );
                List<Comment> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Node cNode = record.get("c").asNode();
                    Node uNode = record.get("u").asNode();
                    User author = userRepository.mapNodeToUser(uNode);
                    list.add(mapNodeToComment(cNode, author, post));
                }
                return list;
            });
        }
    }

    public List<Comment> findByPost(Post post) {
        if (post == null || post.getId() == null) return Collections.emptyList();
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Post {id: $postId})-[:HAS_COMMENT]->(c:Comment) " +
                    "MATCH (u:User)-[:COMMENTED]->(c) " +
                    "RETURN c, u",
                    Values.parameters("postId", post.getId())
                );
                List<Comment> list = new ArrayList<>();
                while (result.hasNext()) {
                    var record = result.next();
                    Node cNode = record.get("c").asNode();
                    Node uNode = record.get("u").asNode();
                    User author = userRepository.mapNodeToUser(uNode);
                    list.add(mapNodeToComment(cNode, author, post));
                }
                return list;
            });
        }
    }

    public long countByPost(Post post) {
        if (post == null || post.getId() == null) return 0;
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Post {id: $postId})-[:HAS_COMMENT]->(c:Comment) RETURN count(c) AS count",
                    Values.parameters("postId", post.getId())
                );
                if (result.hasNext()) {
                    return result.next().get("count").asLong();
                }
                return 0L;
            });
        }
    }

    public Comment save(Comment comment) {
        if (comment.getId() == null) {
            comment.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        }
        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(LocalDateTime.now());
        }
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MERGE (c:Comment {id: $id}) " +
                    "SET c.content = $content, " +
                    "    c.createdAt = $createdAt " +
                    "WITH c " +
                    "MATCH (p:Post {id: $postId}) " +
                    "MERGE (p)-[:HAS_COMMENT]->(c) " +
                    "WITH c " +
                    "MATCH (u:User {id: $authorId}) " +
                    "MERGE (u)-[:COMMENTED]->(c) " +
                    "WITH c " +
                    "OPTIONAL MATCH (parent:Comment {id: $parentId}) " +
                    "FOREACH (ignoreMe IN CASE WHEN parent IS NOT NULL THEN [1] ELSE [] END | " +
                    "    MERGE (parent)-[:HAS_REPLY]->(c) " +
                    ") " +
                    "RETURN c",
                    Values.parameters(
                        "id", comment.getId(),
                        "content", comment.getContent() != null ? comment.getContent() : "",
                        "createdAt", comment.getCreatedAt().toString(),
                        "postId", comment.getPost().getId(),
                        "authorId", comment.getAuthor().getId(),
                        "parentId", comment.getParentComment() != null ? comment.getParentComment().getId() : null
                    )
                );
                return null;
            });
        }
        return comment;
    }

    public Optional<Comment> findById(Long id) {
        try (Session session = driver.session()) {
            return session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (p:Post)-[:HAS_COMMENT]->(c:Comment {id: $id}) " +
                    "MATCH (u:User)-[:COMMENTED]->(c) " +
                    "RETURN c, u, p",
                    Values.parameters("id", id)
                );
                if (result.hasNext()) {
                    var record = result.next();
                    Node cNode = record.get("c").asNode();
                    Node uNode = record.get("u").asNode();
                    Node pNode = record.get("p").asNode();
                    User author = userRepository.mapNodeToUser(uNode);
                    
                    Post post = new Post();
                    post.setId(pNode.get("id").asLong());
                    
                    return Optional.of(mapNodeToComment(cNode, author, post));
                }
                return Optional.empty();
            });
        }
    }

    public void delete(Comment comment) {
        if (comment == null || comment.getId() == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(
                    "MATCH (c:Comment {id: $id}) DETACH DELETE c",
                    Values.parameters("id", comment.getId())
                );
                return null;
            });
        }
    }

    public void deleteAll(Iterable<? extends Comment> entities) {
        if (entities == null) return;
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                for (Comment c : entities) {
                    tx.run(
                        "MATCH (c:Comment {id: $id}) DETACH DELETE c",
                        Values.parameters("id", c.getId())
                    );
                }
                return null;
            });
        }
    }

    public void flush() {
        // No-op for Neo4j as it executes write transactions immediately
    }

    private Comment mapNodeToComment(Node node, User author, Post post) {
        Comment comment = new Comment();
        comment.setId(node.get("id").asLong());
        comment.setAuthor(author);
        comment.setPost(post);
        comment.setContent(node.get("content").asString());
        if (node.containsKey("createdAt") && !node.get("createdAt").isNull()) {
            comment.setCreatedAt(LocalDateTime.parse(node.get("createdAt").asString()));
        }

        // Fetch parentComment if exists
        try (Session session = driver.session()) {
            session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (parent:Comment)-[:HAS_REPLY]->(c:Comment {id: $commentId}) RETURN parent",
                    Values.parameters("commentId", comment.getId())
                );
                if (result.hasNext()) {
                    Node pNode = result.next().get("parent").asNode();
                    Comment parent = new Comment();
                    parent.setId(pNode.get("id").asLong());
                    comment.setParentComment(parent);
                }
                return null;
            });
        }

        // Fetch replies if exists
        List<Comment> replies = new ArrayList<>();
        try (Session session = driver.session()) {
            session.executeRead(tx -> {
                var result = tx.run(
                    "MATCH (c:Comment {id: $commentId})-[:HAS_REPLY]->(reply:Comment) MATCH (ru:User)-[:COMMENTED]->(reply) RETURN reply, ru",
                    Values.parameters("commentId", comment.getId())
                );
                while (result.hasNext()) {
                    var record = result.next();
                    Node rNode = record.get("reply").asNode();
                    Node ruNode = record.get("ru").asNode();
                    User rAuthor = userRepository.mapNodeToUser(ruNode);
                    replies.add(mapNodeToComment(rNode, rAuthor, post));
                }
                return null;
            });
        }
        comment.setReplies(replies);

        return comment;
    }
}
