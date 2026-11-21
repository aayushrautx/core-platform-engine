package com.coreplatform.authorization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
public class ServiceAccountData {

    private UUID id;
    private String keycloakSub;
    private UUID tenantId;
    private String name;
    private List<UUID> roleIds;
    private Set<String> effectivePermissions;
    private long version;
}
