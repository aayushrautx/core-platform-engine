package com.coreplatform.authorization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Role {

    private UUID id;
    private UUID tenantId;
    private String name;
    private UUID parentRoleId;
    private List<String> permissionKeys;
    private Map<String, String> scopeByPermission;
    private boolean isSystem;
    private boolean isActive;
    private long version;
}
