package com.coreplatform.users.service;

import com.coreplatform.authorization.model.CombiningAlgorithm;
import com.coreplatform.authorization.model.PolicyEffect;
import com.coreplatform.users.dto.*;
import com.coreplatform.users.entity.PolicyEntity;
import com.coreplatform.users.entity.PolicyHistoryEntity;
import com.coreplatform.users.entity.PolicySetEntity;
import com.coreplatform.users.repository.PolicyHistoryRepository;
import com.coreplatform.users.repository.PolicyRepository;
import com.coreplatform.users.repository.PolicySetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PolicyService {

    private static final int MAX_POLICY_SETS_PER_TENANT = 100;
    private static final int MAX_POLICIES_PER_SET = 50;
    private static final int MAX_CONDITIONS_JSON_SIZE = 16384;

    private final PolicySetRepository policySetRepository;
    private final PolicyRepository policyRepository;
    private final PolicyHistoryRepository policyHistoryRepository;
    private final OutboxService outboxService;
    private final ChangeLogService changeLogService;
    private final ObjectMapper objectMapper;

    public PolicyService(PolicySetRepository policySetRepository,
                         PolicyRepository policyRepository,
                         PolicyHistoryRepository policyHistoryRepository,
                         OutboxService outboxService,
                         ChangeLogService changeLogService,
                         ObjectMapper objectMapper) {
        this.policySetRepository = policySetRepository;
        this.policyRepository = policyRepository;
        this.policyHistoryRepository = policyHistoryRepository;
        this.outboxService = outboxService;
        this.changeLogService = changeLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<PolicySetResponse> listPolicySets(UUID tenantId) {
        return policySetRepository.findByTenantIdWithPolicies(tenantId).stream()
                .map(this::toPolicySetResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<PolicySetResponse> getPolicySet(UUID id) {
        return policySetRepository.findById(id).map(this::toPolicySetResponse);
    }

    @Transactional
    public PolicySetResponse createPolicySet(CreatePolicySetRequest request, UUID tenantId, UUID actorId) {
        long count = policySetRepository.countByTenantId(tenantId);
        if (count >= MAX_POLICY_SETS_PER_TENANT) {
            throw new IllegalStateException("Max policy sets per tenant reached: " + MAX_POLICY_SETS_PER_TENANT);
        }

        validateCombiningStrategy(request.combiningStrategy());

        PolicySetEntity entity = PolicySetEntity.builder()
                .tenantId(tenantId)
                .name(request.name())
                .description(request.description())
                .targetResource(request.targetResource())
                .targetAction(request.targetAction())
                .combiningStrategy(request.combiningStrategy() != null ? request.combiningStrategy() : "DENY_OVERRIDES")
                .isActive(true)
                .version(1)
                .createdBy(actorId)
                .build();

        entity = policySetRepository.save(entity);
        outboxService.writeEvent("POLICY_SET_CREATED", tenantId, "POLICY_SET", entity.getId(), entity.getVersion(), null);
        changeLogService.logChange(actorId, "POLICY_SET", entity.getId(), "CREATED", null, entity.getName());
        log.info("Created policy set: id={}, name={}, tenant={}", entity.getId(), entity.getName(), tenantId);
        return toPolicySetResponse(entity);
    }

    @Transactional
    public Optional<PolicySetResponse> updatePolicySet(UUID id, UpdatePolicySetRequest request, UUID actorId) {
        return policySetRepository.findById(id).map(entity -> {
            String oldState = entity.getCombiningStrategy();
            if (request.description() != null) entity.setDescription(request.description());
            if (request.combiningStrategy() != null) {
                validateCombiningStrategy(request.combiningStrategy());
                entity.setCombiningStrategy(request.combiningStrategy());
            }
            if (request.isActive() != null) entity.setActive(request.isActive());
            entity = policySetRepository.save(entity);
            outboxService.writeEvent("POLICY_SET_UPDATED", entity.getTenantId(), "POLICY_SET", entity.getId(), entity.getVersion(), null);
            changeLogService.logChange(actorId, "POLICY_SET", entity.getId(), "UPDATED", oldState, entity.getCombiningStrategy());
            return toPolicySetResponse(entity);
        });
    }

    @Transactional
    public Optional<PolicySetResponse> deletePolicySet(UUID id, UUID actorId) {
        return policySetRepository.findById(id).map(entity -> {
            entity.setActive(false);
            entity = policySetRepository.save(entity);
            outboxService.writeEvent("POLICY_SET_DELETED", entity.getTenantId(), "POLICY_SET", entity.getId(), entity.getVersion(), null);
            changeLogService.logChange(actorId, "POLICY_SET", entity.getId(), "DEACTIVATED", null, null);
            log.info("Deactivated policy set: id={}", id);
            return toPolicySetResponse(entity);
        });
    }

    @Transactional
    public PolicyResponse createPolicy(UUID policySetId, CreatePolicyRequest request, UUID actorId) {
        PolicySetEntity policySet = policySetRepository.findById(policySetId)
                .orElseThrow(() -> new NoSuchElementException("PolicySet not found: " + policySetId));

        long count = policyRepository.countByPolicySetId(policySetId);
        if (count >= MAX_POLICIES_PER_SET) {
            throw new IllegalStateException("Max policies per set reached: " + MAX_POLICIES_PER_SET);
        }

        validateEffect(request.effect());
        validateConditionsJson(request.conditionsJson());

        PolicyEntity entity = PolicyEntity.builder()
                .policySet(policySet)
                .name(request.name())
                .description(request.description())
                .effect(request.effect())
                .overrideRbac(request.overrideRbac())
                .conditionsJson(request.conditionsJson())
                .obligationsJson(request.obligationsJson())
                .priority(request.priority())
                .isActive(true)
                .version(1)
                .createdBy(actorId)
                .build();

        entity = policyRepository.save(entity);
        recordHistory(entity, policySet.getTenantId(), "CREATED", null, actorId);
        outboxService.writeEvent("POLICY_CREATED", policySet.getTenantId(), "POLICY", entity.getId(), entity.getVersion(), null);
        changeLogService.logChange(actorId, "POLICY", entity.getId(), "CREATED", null, entity.getName());
        log.info("Created policy: id={}, name={}, policySet={}", entity.getId(), entity.getName(), policySetId);
        return toPolicyResponse(entity);
    }

    @Transactional
    public Optional<PolicyResponse> updatePolicy(UUID id, UpdatePolicyRequest request, UUID actorId) {
        return policyRepository.findById(id).map(entity -> {
            String oldValue = serializePolicy(entity);

            if (request.description() != null) entity.setDescription(request.description());
            if (request.conditionsJson() != null) {
                validateConditionsJson(request.conditionsJson());
                entity.setConditionsJson(request.conditionsJson());
            }
            if (request.obligationsJson() != null) entity.setObligationsJson(request.obligationsJson());
            if (request.priority() != null) entity.setPriority(request.priority());
            if (request.isActive() != null) entity.setActive(request.isActive());

            entity = policyRepository.save(entity);
            recordHistory(entity, entity.getPolicySet().getTenantId(), "UPDATED", oldValue, actorId);
            outboxService.writeEvent("POLICY_UPDATED", entity.getPolicySet().getTenantId(), "POLICY", entity.getId(), entity.getVersion(), null);
            changeLogService.logChange(actorId, "POLICY", entity.getId(), "UPDATED", oldValue, serializePolicy(entity));
            return toPolicyResponse(entity);
        });
    }

    @Transactional
    public Optional<PolicyResponse> deletePolicy(UUID id, UUID actorId) {
        return policyRepository.findById(id).map(entity -> {
            String oldValue = serializePolicy(entity);
            entity.setActive(false);
            entity = policyRepository.save(entity);
            recordHistory(entity, entity.getPolicySet().getTenantId(), "DEACTIVATED", oldValue, actorId);
            outboxService.writeEvent("POLICY_DELETED", entity.getPolicySet().getTenantId(), "POLICY", entity.getId(), entity.getVersion(), null);
            changeLogService.logChange(actorId, "POLICY", entity.getId(), "DEACTIVATED", oldValue, null);
            log.info("Deactivated policy: id={}", id);
            return toPolicyResponse(entity);
        });
    }

    @Transactional(readOnly = true)
    public Optional<PolicyResponse> getPolicy(UUID id) {
        return policyRepository.findById(id).map(this::toPolicyResponse);
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> listPolicies(UUID policySetId) {
        return policyRepository.findByPolicySetId(policySetId).stream()
                .map(this::toPolicyResponse)
                .collect(Collectors.toList());
    }

    private void validateCombiningStrategy(String strategy) {
        if (strategy == null) return;
        try {
            CombiningAlgorithm.valueOf(strategy);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid combining strategy: " + strategy +
                    ". Valid values: " + Arrays.toString(CombiningAlgorithm.values()));
        }
    }

    private void validateEffect(String effect) {
        try {
            PolicyEffect.valueOf(effect);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid effect: " + effect + ". Valid values: PERMIT, DENY");
        }
    }

    private void validateConditionsJson(String conditionsJson) {
        if (conditionsJson == null || conditionsJson.isBlank()) return;
        if (conditionsJson.length() > MAX_CONDITIONS_JSON_SIZE) {
            throw new IllegalArgumentException("Conditions JSON exceeds max size of " + MAX_CONDITIONS_JSON_SIZE + " bytes");
        }
        try {
            objectMapper.readTree(conditionsJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid conditions JSON: " + e.getMessage());
        }
    }

    private void recordHistory(PolicyEntity policy, UUID tenantId, String action, String oldValue, UUID actorId) {
        PolicyHistoryEntity history = PolicyHistoryEntity.builder()
                .policyId(policy.getId())
                .policySetId(policy.getPolicySet().getId())
                .tenantId(tenantId)
                .action(action)
                .oldValue(oldValue)
                .newValue(serializePolicy(policy))
                .changedBy(actorId)
                .build();
        policyHistoryRepository.save(history);
    }

    private String serializePolicy(PolicyEntity entity) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", entity.getName());
            data.put("effect", entity.getEffect());
            data.put("overrideRbac", entity.isOverrideRbac());
            data.put("conditionsJson", entity.getConditionsJson());
            data.put("obligationsJson", entity.getObligationsJson());
            data.put("priority", entity.getPriority());
            data.put("isActive", entity.isActive());
            data.put("version", entity.getVersion());
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{}";
        }
    }

    private PolicySetResponse toPolicySetResponse(PolicySetEntity entity) {
        List<PolicyResponse> policies = entity.getPolicies() != null
                ? entity.getPolicies().stream().map(this::toPolicyResponse).collect(Collectors.toList())
                : Collections.emptyList();

        return new PolicySetResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTargetResource(),
                entity.getTargetAction(),
                entity.getCombiningStrategy(),
                entity.isActive(),
                entity.getVersion(),
                policies,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PolicyResponse toPolicyResponse(PolicyEntity entity) {
        return new PolicyResponse(
                entity.getId(),
                entity.getPolicySet().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getEffect(),
                entity.isOverrideRbac(),
                entity.getConditionsJson(),
                entity.getObligationsJson(),
                entity.getPriority(),
                entity.isActive(),
                entity.getVersion(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
