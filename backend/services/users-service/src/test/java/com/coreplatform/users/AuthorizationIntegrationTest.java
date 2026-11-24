package com.coreplatform.users;

import com.coreplatform.users.dto.*;
import com.coreplatform.users.entity.UserRoleEntity;
import com.coreplatform.users.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthorizationIntegrationTest {

    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";
    private static final String SUPER_ADMIN_ROLE_ID = "00000000-0000-0000-0000-000000000010";
    private static final String ADMIN_ROLE_ID = "00000000-0000-0000-0000-000000000011";
    private static final String VIEWER_ROLE_ID = "00000000-0000-0000-0000-000000000012";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRoleRepository userRoleRepository;

    private static String adminSub;
    private static String adminUserId;
    private static String testUserSub;
    private static String testUserId;
    private static String createdRoleId;
    private static String createdPermissionId;

    // ==================== Health & Actuator ====================

    @Test
    @Order(1)
    void healthEndpoint_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(2)
    void metricsEndpoint_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }

    // ==================== UserSyncFilter (auto-create on first login) ====================

    @Test
    @Order(9)
    void bootstrapAdmin_shouldAutoCreateAndAssignSuperAdmin() throws Exception {
        adminSub = UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j
                                .subject(adminSub)
                                .claim("preferred_username", "integration_admin")
                                .claim("email", "integration_admin@test.com")
                                .claim("tenantId", DEFAULT_TENANT_ID))))
                .andExpect(status().isOk())
                .andReturn();

        adminUserId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();

        userRoleRepository.save(UserRoleEntity.builder()
                .userId(UUID.fromString(adminUserId))
                .roleId(UUID.fromString(SUPER_ADMIN_ROLE_ID))
                .assignedAt(Instant.now())
                .assignedBy(UUID.fromString(adminUserId))
                .build());
    }

    @Test
    @Order(10)
    void firstLogin_shouldAutoCreateUser() throws Exception {
        testUserSub = UUID.randomUUID().toString();

        MvcResult result = mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j
                                .subject(testUserSub)
                                .claim("preferred_username", "integration_testuser")
                                .claim("email", "integration_testuser@test.com")
                                .claim("tenantId", DEFAULT_TENANT_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakSub").value(testUserSub))
                .andExpect(jsonPath("$.username").value("integration_testuser"))
                .andReturn();

        testUserId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    @Order(11)
    void secondLogin_shouldReturnExistingUser() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .with(jwt().jwt(j -> j
                                .subject(testUserSub)
                                .claim("preferred_username", "integration_testuser")
                                .claim("email", "integration_testuser@test.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakSub").value(testUserSub));
    }

    // ==================== Roles CRUD ====================

    @Test
    @Order(20)
    void listRoles_shouldReturnSeededRoles() throws Exception {
        mockMvc.perform(get("/api/roles")
                        .param("tenantId", DEFAULT_TENANT_ID)
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$[?(@.name == 'super_admin')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'admin')]").exists())
                .andExpect(jsonPath("$[?(@.name == 'viewer')]").exists());
    }

    @Test
    @Order(21)
    void createRole_shouldSucceed() throws Exception {
        assignRoleToTestUser(SUPER_ADMIN_ROLE_ID);

        CreateRoleRequest request = new CreateRoleRequest("it_hr_manager_" + System.currentTimeMillis(),
                "HR department manager", UUID.fromString(ADMIN_ROLE_ID));
        MvcResult result = mockMvc.perform(post("/api/roles")
                        .param("tenantId", DEFAULT_TENANT_ID)
                        .with(jwt().jwt(j -> j.subject(testUserSub)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("HR department manager"))
                .andReturn();

        createdRoleId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    @Order(22)
    void getRole_shouldReturnCreatedRole() throws Exception {
        mockMvc.perform(get("/api/roles/" + createdRoleId)
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdRoleId));
    }

    @Test
    @Order(23)
    void updateRole_shouldSucceed() throws Exception {
        UpdateRoleRequest request = new UpdateRoleRequest("it_hr_lead_" + System.currentTimeMillis(),
                "HR department lead", UUID.fromString(ADMIN_ROLE_ID));
        mockMvc.perform(put("/api/roles/" + createdRoleId)
                        .with(jwt().jwt(j -> j.subject(testUserSub)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("HR department lead"));
    }

    // ==================== Permissions CRUD ====================

    @Test
    @Order(30)
    void listPermissions_shouldReturnSeededPermissions() throws Exception {
        mockMvc.perform(get("/api/permissions")
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(10))));
    }

    @Test
    @Order(31)
    void createPermission_shouldSucceed() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest(
                "report", "generate_" + System.currentTimeMillis(), "Generate reports");
        MvcResult result = mockMvc.perform(post("/api/permissions")
                        .with(jwt().jwt(j -> j.subject(testUserSub)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resource").value("report"))
                .andReturn();

        createdPermissionId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    @Order(32)
    void assignPermissionToRole_shouldSucceed() throws Exception {
        RolePermissionRequest request = new RolePermissionRequest(UUID.fromString(createdPermissionId), "all");
        mockMvc.perform(post("/api/roles/" + createdRoleId + "/permissions")
                        .with(jwt().jwt(j -> j.subject(adminSub)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(33)
    void removePermissionFromRole_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/roles/" + createdRoleId + "/permissions/" + createdPermissionId)
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isNoContent());
    }

    // ==================== User Roles ====================

    @Test
    @Order(40)
    void getUserRoles_shouldReturnAssignedRoles() throws Exception {
        mockMvc.perform(get("/api/users/" + testUserId + "/roles")
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(41)
    void assignRole_shouldSucceed() throws Exception {
        AssignRoleRequest request = new AssignRoleRequest(UUID.fromString(createdRoleId));
        mockMvc.perform(post("/api/users/" + testUserId + "/roles")
                        .with(jwt().jwt(j -> j.subject(adminSub)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @Order(42)
    void removeRoleFromUser_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/users/" + testUserId + "/roles/" + createdRoleId)
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isNoContent());
    }

    // ==================== Users CRUD ====================

    @Test
    @Order(50)
    void listUsers_shouldReturnUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .param("tenantId", DEFAULT_TENANT_ID)
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @Order(51)
    void updateUser_shouldSucceed() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest("integration_testuser_updated", "integration_testuser@test.com",
                null, "Engineering", "Senior Developer");
        mockMvc.perform(put("/api/users/" + testUserId)
                        .with(jwt().jwt(j -> j.subject(testUserSub)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("integration_testuser_updated"))
                .andExpect(jsonPath("$.department").value("Engineering"));
    }

    // ==================== Service Accounts ====================

    @Test
    @Order(60)
    void createServiceAccount_shouldSucceed() throws Exception {
        CreateServiceAccountRequest request = new CreateServiceAccountRequest(
                "sa-integration-" + System.currentTimeMillis(), "integration-test-service",
                UUID.fromString(DEFAULT_TENANT_ID));
        mockMvc.perform(post("/api/service-accounts")
                        .with(jwt().jwt(j -> j.subject(testUserSub)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("integration-test-service"));
    }

    @Test
    @Order(61)
    void listServiceAccounts_shouldReturnAccounts() throws Exception {
        mockMvc.perform(get("/api/service-accounts")
                        .param("tenantId", DEFAULT_TENANT_ID)
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // ==================== Internal AuthzData ====================

    @Test
    @Order(65)
    void internalGetUserData_shouldReturnData() throws Exception {
        mockMvc.perform(get("/api/internal/authz-data/user/" + testUserSub)
                        .with(jwt().jwt(j -> j
                                .subject(testUserSub)
                                .claim("azp", "users-service"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakSub").value(testUserSub));
    }

    // ==================== Metrics Verification ====================

    @Test
    @Order(70)
    void metricsEndpoint_shouldHaveDecisionMetric() throws Exception {
        mockMvc.perform(get("/actuator/metrics/authz.decision.duration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("authz.decision.duration"));
    }

    // ==================== Role Deletion (soft-delete, last test) ====================

    @Test
    @Order(80)
    void deleteRole_shouldSoftDelete() throws Exception {
        mockMvc.perform(delete("/api/roles/" + createdRoleId)
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/roles/" + createdRoleId)
                        .with(jwt().jwt(j -> j.subject(testUserSub))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    // ==================== Helper Methods ====================

    private void assignRoleToTestUser(String roleId) {
        userRoleRepository.save(UserRoleEntity.builder()
                .userId(UUID.fromString(testUserId))
                .roleId(UUID.fromString(roleId))
                .assignedAt(Instant.now())
                .assignedBy(UUID.fromString(adminUserId))
                .build());
    }
}
