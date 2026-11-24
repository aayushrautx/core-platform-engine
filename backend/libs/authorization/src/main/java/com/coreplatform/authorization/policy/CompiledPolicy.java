package com.coreplatform.authorization.policy;

import com.coreplatform.authorization.model.Policy;
import com.coreplatform.authorization.model.PolicyEffect;
import lombok.Getter;

import java.util.UUID;

@Getter
public class CompiledPolicy {

    private final UUID id;
    private final String name;
    private final PolicyEffect effect;
    private final boolean overrideRbac;
    private final ConditionNode conditionTree;
    private final int priority;
    private final long version;

    public CompiledPolicy(Policy policy, ConditionNode conditionTree) {
        this.id = policy.getId();
        this.name = policy.getName();
        this.effect = policy.getEffect();
        this.overrideRbac = policy.isOverrideRbac();
        this.conditionTree = conditionTree;
        this.priority = policy.getPriority();
        this.version = policy.getVersion();
    }
}
