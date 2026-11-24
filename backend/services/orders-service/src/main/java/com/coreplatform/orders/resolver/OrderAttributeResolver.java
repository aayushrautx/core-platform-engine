package com.coreplatform.orders.resolver;

import com.coreplatform.authorization.resolver.ResourceAttributeResolver;
import com.coreplatform.orders.entity.OrderEntity;
import com.coreplatform.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderAttributeResolver implements ResourceAttributeResolver {

    private final OrderRepository orderRepository;

    @Override
    public String supportedResource() {
        return "order";
    }

    @Override
    public Map<String, Object> resolve(String resource, Object resourceId) {
        UUID orderId = resourceId instanceof UUID ? (UUID) resourceId : UUID.fromString(resourceId.toString());

        return orderRepository.findById(orderId)
                .map(this::toAttributes)
                .orElse(Map.of());
    }

    private Map<String, Object> toAttributes(OrderEntity order) {
        return Map.of(
                "ownerId", order.getOwnerKeycloakSub(),
                "department", order.getDepartment(),
                "tenantId", order.getTenantId().toString(),
                "amount", order.getAmount(),
                "status", order.getStatus()
        );
    }
}
