package com.uday.rguktconnect.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class DatabaseConfig {

    @Value("${cognodb.url:bolt://localhost:7687}")
    private String congoUrl;

    @Value("${cognodb.username:cognodb}")
    private String congoUsername;

    @Value("${cognodb.password:password}")
    private String congoPassword;

    @Bean
    public Driver neo4jDriver() {
        String uri = "bolt://localhost:7687";
        String username = "cognodb";
        String password = "password";

        if (congoUrl != null && !congoUrl.trim().isEmpty()) {
            uri = congoUrl.trim();
        }
        if (congoUsername != null && !congoUsername.trim().isEmpty()) {
            username = congoUsername.trim();
        }
        if (congoPassword != null && !congoPassword.trim().isEmpty()) {
            password = congoPassword.trim();
        }
 
        if (uri.startsWith("neo4j://") || uri.startsWith("neo4j+s://") || uri.startsWith("neo4j+ssc://") ||
            uri.startsWith("bolt://") || uri.startsWith("bolt+s://") || uri.startsWith("bolt+ssc://")) {
            try {
                URI parsedUri = new URI(uri);
                String userInfo = parsedUri.getUserInfo();
                if (userInfo != null) {
                    String[] parts = userInfo.split(":");
                    username = parts[0];
                    if (parts.length > 1) {
                        password = parts[1];
                    }
                    uri = parsedUri.getScheme() + "://" + parsedUri.getHost() + (parsedUri.getPort() != -1 ? ":" + parsedUri.getPort() : "");
                }
            } catch (Exception e) {
                // Ignore and use default URI
            }
        }

        System.out.println("Connecting to CognoDB Graph DB: " + uri + " (user: " + username + ")");
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
}
