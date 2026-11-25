package com.coreplatform.authorization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Policy {

    private UUID id;
    private String name;
    private PolicyEffect effect;
    private boolean overrideRbac;
    private String conditionsJson;
    private String obligationsJson;
    private int priority;
    @JsonProperty("isActive")
    private boolean isActive;
    private long version;
    private CombiningAlgorithm combiningAlgorithm;
}
