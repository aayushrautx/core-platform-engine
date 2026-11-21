package com.coreplatform.authorization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PolicySet {

    private UUID id;
    private UUID tenantId;
    private String targetResource;
    private String targetAction;
    private CombiningAlgorithm combiningStrategy;
    private List<Policy> policies;
    private long version;
}
