package com.hometusk.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hometusk.households.domain.Household;
import com.hometusk.households.domain.Zone;
import com.hometusk.households.repository.HouseholdRepository;
import com.hometusk.households.repository.ZoneRepository;
import com.hometusk.users.domain.Membership;
import com.hometusk.users.domain.MembershipRole;
import com.hometusk.users.domain.User;
import com.hometusk.users.repository.MembershipRepository;
import com.hometusk.users.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    // Singleton container pattern - container is shared across ALL test classes
    static PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                .withDatabaseName("hometusk_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Disable Keycloak for tests - use mock JWT
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost:8180/realms/test");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected HouseholdRepository householdRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MembershipRepository membershipRepository;

    @Autowired
    protected ZoneRepository zoneRepository;

    // Test data
    protected Household testHousehold;
    protected User testUser;
    protected User testUser2;
    protected Membership testMembership;
    protected Zone testZone;

    @BeforeEach
    void setUpTestData() {
        // Use unique IDs per test to avoid collisions with singleton container
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);

        // Create test household
        testHousehold = new Household("Test Household " + uniqueSuffix);
        testHousehold = householdRepository.save(testHousehold);

        // Create test users with unique externalId
        testUser = new User("test-user-" + uniqueSuffix + "-1", "alice-" + uniqueSuffix + "@test.local", "Alice Test");
        testUser = userRepository.save(testUser);

        testUser2 = new User("test-user-" + uniqueSuffix + "-2", "bob-" + uniqueSuffix + "@test.local", "Bob Test");
        testUser2 = userRepository.save(testUser2);

        // Create membership for testUser
        testMembership = new Membership(testUser, testHousehold, MembershipRole.admin);
        testMembership = membershipRepository.save(testMembership);

        // Create test zone
        testZone = new Zone(testHousehold, "Kitchen");
        testZone = zoneRepository.save(testZone);
    }

    /**
     * Creates a JWT mock for the given user.
     */
    protected RequestPostProcessor jwtForUser(User user) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(jwt -> jwt.subject(user.getExternalId())
                .claim("email", user.getEmail())
                .claim("name", user.getDisplayName()));
    }

    /**
     * Creates a JWT mock for the test user.
     */
    protected RequestPostProcessor jwt() {
        return jwtForUser(testUser);
    }

    /**
     * Generates a random correlation ID.
     */
    protected String randomCorrelationId() {
        return UUID.randomUUID().toString();
    }
}
