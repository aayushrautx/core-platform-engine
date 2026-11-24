package com.coreplatform.authorization.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConditionNode {

    private ConditionOperator operator;
    private String attribute;
    private Object value;
    private List<ConditionNode> children;

    public boolean isLogical() {
        return operator == ConditionOperator.AND || operator == ConditionOperator.OR;
    }
}
