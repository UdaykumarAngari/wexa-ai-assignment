# Campus Connect 🌐

Campus Connect is a premium professional networking platform tailored for **Rajiv Gandhi University of Knowledge Technologies (RGUKT)** students and alumni. Built with a high-performance modern stack (Spring Boot + React + CognoDB/Neo4j), it enables students to seek mentorship, referrals, and career advice directly from verified alumni in a highly interactive social ecosystem.

---

## 🏛️ Architectural Graph Selection: Why a Graph Database?

Campus Connect deals with highly connected social and professional data. Traditional Relational Database Management Systems (RDBMS) or Document Databases (NoSQL) fail to scale efficiently for these workloads. Here is why **Neo4j / CognoDB** is the correct choice:

1. **Eliminating Exponential Join Penalties (Index-Free Adjacency)**:
   In SQL, determining if two users are connected through a mutual friend requires a recursive self-join on a `connections` table. For $N$-degree separations (e.g. "People You May Know"), SQL queries suffer from exponential performance degradation because they rely on global index lookups. Neo4j uses *Index-Free Adjacency*, meaning each node holds direct pointers to its neighbors. Traversing connections is $O(1)$ per hop, regardless of the database size.
   
2. **Dynamic & Unstructured Profiles**:
   Alumni profiles are highly dynamic: one user might have three different internship experiences, another might have none, and others might list academic projects, certifications, or research papers. In RDBMS, this requires multiple joined tables and nullable fields. In a graph, user details and experience attributes are dynamic nodes/relationships created on-the-fly without database migrations or rigid schemas.

3. **Natural Expression of Social Networks**:
   Writing recommendation logic in SQL is verbose, error-prone, and slow. In Neo4j, we can easily declare complex path patterns using the intuitive **Cypher Query Language**.

---

## 📊 Graph Data Model (CognoDB / Neo4j Schema)

Below is the graph schema representing the node labels, attributes, and relationships. It contains no relational foreign key tables, relying purely on native graph relationships:

```mermaid
graph TD
    User["User Node
    • id: Long
    • idNumber: String
    • name: String
    • universityEmail: String
    • role: String
    • password: String"]
    
    UserDetails["UserDetails Node
    • branch: String
    • batch: String
    • mobileNumber: String
    • description: String
    • profilePhoto: String
    • coverPhoto: String
    • mentoredStudentsCount: Integer"]

    UserExperience["UserExperience Node
    • title: String
    • companyName: String
    • location: String
    • employmentType: String
    • locationType: String
    • startDate: LocalDate
    • endDate: LocalDate
    • currentRole: Boolean
    • description: String"]

    Post["Post Node
    • id: Long
    • type: String (GENERAL/REFERRAL)
    • content: String
    • company: String (Optional)
    • role: String (Optional)
    • createdAt: LocalDateTime"]

    Comment["Comment Node
    • id: Long
    • content: String
    • createdAt: LocalDateTime"]

    ChatMessage["ChatMessage Node
    • id: Long
    • content: String
    • isRead: Boolean
    • timestamp: LocalDateTime"]

    Job["Job Node
    • id: Long
    • company: String
    • role: String
    • location: String
    • salary: String
    • applyUrl: String
    • type: String (Full-time/Intern)
    • category: String
    • referralAvailable: Boolean
    • expiresAt: LocalDate
    • createdAt: LocalDateTime"]

    Notification["Notification Node
    • id: Long
    • type: String
    • relatedId: Long
    • isRead: Boolean
    • createdAt: LocalDateTime"]

    %% Relationships
    User -->|HAS_DETAILS| UserDetails
    User -->|HAS_EXPERIENCE| UserExperience
    User -->|AUTHORED| Post
    User -->|LIKED| Post
    Post -->|HAS_COMMENT| Comment
    User -->|COMMENTED| Comment
    Comment -->|HAS_REPLY| Comment
    User -->|SENT_MESSAGE| ChatMessage
    ChatMessage -->|RECEIVED_BY| User
    User -->|TRIGGERED_NOTIFICATION| Notification
    Notification -->|RECEIVED_NOTIFICATION| User
    User -->|POSTED| Job
    User -->|CONNECTED {status: 'ACCEPTED'/'PENDING'}| User
```

---

## ⚡ Primary Cypher Traversal Queries

### 1. Connection Recommendations ("People You May Know")
This query drives the graph-native recommendation engine. It targets 2-hop traversal paths (friends of friends), excludes direct connections, counts mutual friends, collects their names, and returns suggestions ordered by relevance:

```cypher
MATCH (u:User {id: $userId})-[:CONNECTED {status: 'ACCEPTED'}]-(friend:User)-[:CONNECTED {status: 'ACCEPTED'}]-(fof:User)
WHERE NOT (u)-[:CONNECTED]-(fof) AND u.id <> fof.id
OPTIONAL MATCH (fof)-[:HAS_DETAILS]->(d:UserDetails)
RETURN fof, d.profilePhoto AS profilePhoto, count(friend) AS mutualCount, collect(friend.name) AS mutualFriends
ORDER BY mutualCount DESC LIMIT 6
```

### 2. User Directory Filter (Graph Search)
Searches users and their related experiences concurrently, illustrating how the database traverses node boundaries to match criteria:

```cypher
MATCH (u:User)
OPTIONAL MATCH (u)-[:HAS_DETAILS]->(d:UserDetails)
OPTIONAL MATCH (u)-[:HAS_EXPERIENCE]->(e:UserExperience)
WHERE u.name CONTAINS $search OR d.branch CONTAINS $search OR e.companyName CONTAINS $search
RETURN u, d, collect(e) as experiences
```

---

## 🛠️ Local & Production Setup Instructions

### Prerequisites
*   Docker & Docker Compose
*   Java Development Kit (JDK 21)
*   Node.js (v18+)

### Environment Configurations
Create a `.env` file in the root or set these system environment variables:

```bash
# Backend configurations
COGNO_URL=bolt://localhost:7687
COGNO_USERNAME=neo4j
COGNO_PASSWORD=your_password
JWT_SECRET=your_base64_encoded_jwt_secret_key_here

# Frontend configurations
VITE_API_URL=http://localhost:8080
```

### Run using Docker Compose (Recommended)
You can run the entire stack including Neo4j, backend, and frontend with a single command:

```bash
docker-compose up --build -d
```

### Manual Local Development Setup

#### 1. Database Setup
Start a local Neo4j container:
```bash
docker run -d --name campus-connect-db -p 7474:7474 -p 7687:7687 -e NEO4J_AUTH=neo4j/your_password neo4j:latest
```

#### 2. Backend Server Setup
```bash
cd campus-connect/backend
./mvnw clean spring-boot:run
```
*(On startup, if the database is empty, the `DatabaseSeeder` will automatically populate the database with realistic alumni, students, posts, connections, chat history, and job listings).*

#### 3. Frontend Web Client Setup
```bash
cd campus-connect/frontend
npm install
npm run dev
```

---

## 🛡️ Robust Failover & Database Resiliency
*   **Database Seeder**: Instantly seeds default users, accept/pending connections, nested comments, sample chat records, and job listings on application startup if the database is empty.
*   **Global Exception Handling**: Intercepts `Neo4jException` (database server offline/unreachable) at the controller advice layer and returns a clean `503 Service Unavailable` response.
*   **Frontend Downtime Banner**: Displays a non-intrusive, real-time warning card informing the user of the database connectivity status, preserving user experience gracefully.
