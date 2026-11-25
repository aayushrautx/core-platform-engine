package com.coreplatform.authorization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAuthorizationData {

    private UUID userId;
    private String keycloakSub;
    private UUID tenantId;
    private UUID orgId;
    private String department;
    private String designation;
    private List<UUID> roleIds;
    private Set<String> effectivePermissions;
    @JsonProperty("isActive")
    private boolean isActive;
    private long tenantVersion;
    private long version;
}
