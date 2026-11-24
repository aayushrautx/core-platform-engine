package com.coreplatform.authorization.policy;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
public class ConditionEvaluator {

    private static final int MAX_NESTING_DEPTH = 3;

    public boolean evaluate(ConditionNode node, Map<String, Object> context) {
        if (node == null) {
            return true;
        }
        try {
            return evaluateInternal(node, context, 0);
        } catch (Exception e) {
            log.warn("Condition evaluation failed (fail-closed): {}", e.getMessage());
            return false;
        }
    }

    private boolean evaluateInternal(ConditionNode node, Map<String, Object> context, int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            log.warn("Condition nesting depth exceeded (max={}), denying", MAX_NESTING_DEPTH);
            return false;
        }

        if (node.isLogical()) {
            return evaluateLogical(node, context, depth);
        }

        return evaluateComparison(node, context);
    }

    private boolean evaluateLogical(ConditionNode node, Map<String, Object> context, int depth) {
        List<ConditionNode> children = node.getChildren();
        if (children == null || children.isEmpty()) {
            return true;
        }

        if (node.getOperator() == ConditionOperator.AND) {
            for (ConditionNode child : children) {
                if (!evaluateInternal(child, context, depth + 1)) {
                    return false;
                }
            }
            return true;
        }

        if (node.getOperator() == ConditionOperator.OR) {
            for (ConditionNode child : children) {
                if (evaluateInternal(child, context, depth + 1)) {
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateComparison(ConditionNode node, Map<String, Object> context) {
        String attribute = node.getAttribute();
        Object actual = resolveAttribute(attribute, context);
        Object expected = node.getValue();
        ConditionOperator op = node.getOperator();

        switch (op) {
            case IS_NULL:
                return actual == null;
            case IS_NOT_NULL:
                return actual != null;
            case EQUALS:
                return actual != null && toComparable(actual).equals(toComparable(expected));
            case NOT_EQUALS:
                return actual == null || !toComparable(actual).equals(toComparable(expected));
            case IN:
                return actual != null && isIn(actual, expected);
            case NOT_IN:
                return actual == null || !isIn(actual, expected);
            case GREATER_THAN:
                return actual != null && compareNumeric(actual, expected) > 0;
            case LESS_THAN:
                return actual != null && compareNumeric(actual, expected) < 0;
            case BETWEEN:
                return actual != null && evaluateBetween(actual, expected);
            case CONTAINS:
                return actual != null && actual.toString().contains(expected.toString());
            case STARTS_WITH:
                return actual != null && actual.toString().startsWith(expected.toString());
            default:
                log.warn("Unknown operator: {}", op);
                return false;
        }
    }

    private Object resolveAttribute(String attribute, Map<String, Object> context) {
        if (attribute == null || context == null) {
            return null;
        }
        if (attribute.contains(".")) {
            return resolveNestedAttribute(attribute, context);
        }
        return context.get(attribute);
    }

    @SuppressWarnings("unchecked")
    private Object resolveNestedAttribute(String attribute, Map<String, Object> context) {
        String[] parts = attribute.split("\\.", 3);
        Object current = context.get(parts[0]);
        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(parts[i]);
            } else {
                return null;
            }
        }
        return current;
    }

    private Object toComparable(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return value.toString();
    }

    @SuppressWarnings("unchecked")
    private boolean isIn(Object actual, Object expected) {
        if (expected instanceof Collection) {
            Collection<Object> list = (Collection<Object>) expected;
            Object comparable = toComparable(actual);
            return list.stream().map(this::toComparable).anyMatch(comparable::equals);
        }
        return toComparable(actual).equals(toComparable(expected));
    }

    private int compareNumeric(Object actual, Object expected) {
        try {
            BigDecimal a = new BigDecimal(actual.toString());
            BigDecimal b = new BigDecimal(expected.toString());
            return a.compareTo(b);
        } catch (NumberFormatException e) {
            return actual.toString().compareTo(expected.toString());
        }
    }

    @SuppressWarnings("unchecked")
    private boolean evaluateBetween(Object actual, Object expected) {
        if (!(expected instanceof List)) return false;
        List<Object> range = (List<Object>) expected;
        if (range.size() != 2) return false;
        return compareNumeric(actual, range.get(0)) >= 0 && compareNumeric(actual, range.get(1)) <= 0;
    }
}
