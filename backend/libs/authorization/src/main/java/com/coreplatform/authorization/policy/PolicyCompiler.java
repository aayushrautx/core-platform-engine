package com.coreplatform.authorization.policy;

import com.coreplatform.authorization.model.Policy;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class PolicyCompiler {

    private static final int MAX_CONDITION_JSON_SIZE = 16 * 1024;

    private final ObjectMapper objectMapper;
    private final Map<String, CompiledPolicy> compiledCache = new ConcurrentHashMap<>();

    public PolicyCompiler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CompiledPolicy compile(Policy policy) {
        String cacheKey = policy.getId() + ":" + policy.getVersion();

        return compiledCache.computeIfAbsent(cacheKey, k -> doCompile(policy));
    }

    private CompiledPolicy doCompile(Policy policy) {
        ConditionNode conditionTree = parseConditions(policy.getConditionsJson());
        return new CompiledPolicy(policy, conditionTree);
    }

    private ConditionNode parseConditions(String conditionsJson) {
        if (conditionsJson == null || conditionsJson.isBlank()) {
            return null;
        }

        if (conditionsJson.length() > MAX_CONDITION_JSON_SIZE) {
            log.warn("Condition JSON exceeds max size ({}), rejecting", MAX_CONDITION_JSON_SIZE);
            throw new PolicyCompilationException("Condition JSON too large");
        }

        try {
            return objectMapper.readValue(conditionsJson, ConditionNode.class);
        } catch (JsonProcessingException e) {
            throw new PolicyCompilationException("Invalid condition JSON: " + e.getMessage());
        }
    }

    public void evictAll() {
        compiledCache.clear();
    }
}
