package com.uday.rguktconnect.seeder;

import com.uday.rguktconnect.entity.*;
import com.uday.rguktconnect.repository.connection.ConnectionRepository;
import com.uday.rguktconnect.repository.messages.MessageRepository;
import com.uday.rguktconnect.repository.posts.PostRepository;
import com.uday.rguktconnect.repository.user.*;
import com.uday.rguktconnect.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class DatabaseSeeder {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private UserExperienceRepository userExperienceRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void seed() {
        // Only seed if our main seed user or general directory is empty
        if (userRepository.findByUniversityEmail("alumni1@rgukt.ac.in").isPresent()) {
            System.out.println("Database is already seeded. Skipping seeder...");
            return;
        }

        System.out.println("Starting database seeding process...");

        // 1. Create Default Test User
        User testUser = userRepository.findByUniversityEmail("test@rgukt.ac.in").orElseGet(() -> {
            User u = new User();
            u.setIdNumber("TST0001");
            u.setName("Test User");
            u.setUniversityEmail("test@rgukt.ac.in");
            u.setRole("STUDENT");
            u.setPassword(passwordEncoder.encode("test1234"));
            return userRepository.save(u);
        });

        userDetailsRepository.findByUser(testUser).orElseGet(() -> {
            UserDetails ud = new UserDetails();
            ud.setUser(testUser);
            ud.setBranch("CSE");
            ud.setBatch("2022");
            ud.setMobileNumber("9876543210");
            ud.setMentoredStudentsCount(0);
            ud.setDescription("Passionate student interested in graph database architectures.");
            return userDetailsRepository.save(ud);
        });

        // 2. Create Seed Users
        User alumni1 = createUser("B161101", "Rahul Sharma", "alumni1@rgukt.ac.in", "ALUMNI", "password123");
        createUserDetails(alumni1, "CSE", "2020", "9876543201", "Software Engineer at Google. Willing to mentor juniors on interview prep.");
        createUserExperience(alumni1, "Software Engineer", "Google", "Hyderabad", "Full-time", "Hybrid", LocalDate.of(2020, 8, 1), null, true, "Working on distributed systems and cloud infrastructure.");

        User alumni2 = createUser("B171202", "Priya Patel", "alumni2@rgukt.ac.in", "ALUMNI", "password123");
        createUserDetails(alumni2, "ECE", "2021", "9876543202", "Silicon Design Engineer at Intel. Always open to discuss semiconductor trends.");
        createUserExperience(alumni2, "Validation Engineer", "Intel Corporation", "Bengaluru", "Full-time", "On-site", LocalDate.of(2021, 9, 15), null, true, "Validating next-gen processors and hardware designs.");

        User student1 = createUser("B201303", "Amit Kumar", "student1@rgukt.ac.in", "STUDENT", "password123");
        createUserDetails(student1, "CSE", "2024", "9876543203", "Final-year CSE student exploring Full Stack opportunities. Open Source contributor.");

        User student2 = createUser("B211404", "Sneha Reddy", "student2@rgukt.ac.in", "STUDENT", "password123");
        createUserDetails(student2, "CSE", "2025", "9876543204", "Third-year CSE student enthusiastic about Machine Learning and Data Science.");

        // 3. Create Connection relationships
        // A) testUser <-> alumni1 (Rahul Sharma) : ACCEPTED
        createConnection(testUser, alumni1, "ACCEPTED");

        // B) alumni1 (Rahul Sharma) <-> alumni2 (Priya Patel) : ACCEPTED (This forms testUser -[CONNECTED]- Rahul -[CONNECTED]- Priya, making Priya a mutual friend / 2-hop traversal suggestion!)
        createConnection(alumni1, alumni2, "ACCEPTED");

        // C) student1 (Amit Kumar) -> testUser : PENDING
        createConnection(student1, testUser, "PENDING");

        // D) testUser -> student2 (Sneha Reddy) : PENDING (Sent by testUser)
        createConnection(testUser, student2, "PENDING");

        // 4. Create Posts
        createPost(alumni1, "REFERRAL", "Excited to announce that Google is hiring Software Engineering interns for Summer 2027! If you are an RGUKT student or alumnus, DM me for a referral.", "Google", "Software Intern");
        createPost(alumni2, "GENERAL", "Had a wonderful time mentoring students at the RGUKT Annual Hackathon this weekend. The coding standards and problem-solving skills shown by the juniors are amazing. Kudos!", null, null);
        createPost(student1, "GENERAL", "Successfully deployed my first Spring Boot application backed by CognoDB graph database. Cypher queries are so much cleaner than recursive SQL self-joins! #GraphDB #WebDev", null, null);

        // 5. Create Chat Messages
        createChatMessage(alumni1, testUser, "Hello! Thanks for connecting. Let me know if you need any advice regarding resume prep or Google opportunities!");
        createChatMessage(testUser, alumni1, "Thank you so much Rahul! I would love to schedule a brief call whenever you are free to discuss preparation tips.");

        // 6. Create Job Postings
        createJob(alumni1, "Google", "Software Engineer", "Hyderabad", "18-24 LPA", "https://careers.google.com", "Full-time", "Software Development");
        createJob(alumni2, "Intel", "Silicon Validation Engineer", "Bengaluru", "12-16 LPA", "https://intel.com/careers", "Full-time", "Hardware Validation");

        System.out.println("Database seeding completed successfully!");
    }

    private User createUser(String idNumber, String name, String email, String role, String plainPassword) {
        return userRepository.findByUniversityEmail(email).orElseGet(() -> {
            User u = new User();
            u.setIdNumber(idNumber);
            u.setName(name);
            u.setUniversityEmail(email);
            u.setRole(role);
            u.setPassword(passwordEncoder.encode(plainPassword));
            return userRepository.save(u);
        });
    }

    private void createUserDetails(User user, String branch, String batch, String mobile, String desc) {
        userDetailsRepository.findByUser(user).orElseGet(() -> {
            UserDetails ud = new UserDetails();
            ud.setUser(user);
            ud.setBranch(branch);
            ud.setBatch(batch);
            ud.setMobileNumber(mobile);
            ud.setDescription(desc);
            ud.setMentoredStudentsCount(user.getRole().equals("ALUMNI") ? 5 : 0);
            return userDetailsRepository.save(ud);
        });
    }

    private void createUserExperience(User user, String title, String company, String location, String empType, String locType, LocalDate start, LocalDate end, boolean isCurrent, String desc) {
        // Simple check to prevent duplicate experiences
        if (userExperienceRepository.findByUser(user).isEmpty()) {
            UserExperiences ue = new UserExperiences();
            ue.setUser(user);
            ue.setTitle(title);
            ue.setCompanyName(company);
            ue.setLocation(location);
            ue.setEmploymentType(empType);
            ue.setLocationType(locType);
            ue.setStartDate(start);
            ue.setEndDate(end);
            ue.setCurrentRole(isCurrent);
            ue.setDescription(desc);
            userExperienceRepository.save(ue);
        }
    }

    private void createConnection(User sender, User receiver, String status) {
        if (connectionRepository.findConnectionBetweenUsers(sender, receiver).isEmpty()) {
            Connection conn = new Connection();
            conn.setSender(sender);
            conn.setReceiver(receiver);
            conn.setStatus(status);
            connectionRepository.save(conn);
        }
    }

    private void createPost(User author, String type, String content, String company, String jobRole) {
        // Search if author has authored any posts with same content to prevent duplication
        Post post = new Post();
        post.setAuthor(author);
        post.setType(type);
        post.setContent(content);
        post.setCompany(company);
        post.setRole(jobRole);
        post.setCreatedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    private void createChatMessage(User sender, User receiver, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSender(sender);
        msg.setReceiver(receiver);
        msg.setContent(content);
        msg.setRead(true);
        msg.setTimestamp(LocalDateTime.now());
        messageRepository.save(msg);
    }

    private void createJob(User creator, String company, String role, String location, String salary, String applyUrl, String type, String category) {
        Job job = new Job();
        job.setPostedBy(creator);
        job.setCompany(company);
        job.setRole(role);
        job.setLocation(location);
        job.setSalary(salary);
        job.setApplyUrl(applyUrl);
        job.setType(type);
        job.setCategory(category);
        job.setReferralAvailable(true);
        job.setCreatedAt(LocalDateTime.now());
        job.setExpiresAt(LocalDate.now().plusDays(30));
        jobRepository.save(job);
    }
}
