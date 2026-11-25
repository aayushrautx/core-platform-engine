package com.coreplatform.authorization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isSystem")
    private boolean isSystem;
    @JsonProperty("isActive")
    private boolean isActive;
    private long version;
}
