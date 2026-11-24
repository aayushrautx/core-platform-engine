package com.coreplatform.orders.service;

import com.coreplatform.orders.dto.CreateOrderRequest;
import com.coreplatform.orders.dto.OrderResponse;
import com.coreplatform.orders.dto.UpdateOrderRequest;
import com.coreplatform.orders.entity.OrderEntity;
import com.coreplatform.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request,
                                      UUID tenantId,
                                      UUID ownerId,
                                      String ownerKeycloakSub,
                                      String department) {
        Instant now = Instant.now();
        OrderEntity order = OrderEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .ownerId(ownerId)
                .ownerKeycloakSub(ownerKeycloakSub)
                .department(department != null ? department : "default")
                .description(request.description())
                .amount(request.amount())
                .status("DRAFT")
                .createdAt(now)
                .updatedAt(now)
                .build();
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOrders(UUID tenantId, Pageable pageable) {
        return orderRepository.findByTenantId(tenantId, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listOwnOrders(UUID tenantId, String ownerKeycloakSub, Pageable pageable) {
        return orderRepository.findByTenantIdAndOwnerKeycloakSub(tenantId, ownerKeycloakSub, pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> listDepartmentOrders(UUID tenantId, String department, Pageable pageable) {
        return orderRepository.findByTenantIdAndDepartment(tenantId, department, pageable)
                .map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id, UUID tenantId) {
        return orderRepository.findByIdAndTenantId(id, tenantId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public OrderResponse updateOrder(UUID id, UUID tenantId, UpdateOrderRequest request) {
        OrderEntity order = orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(id));

        if (request.description() != null) {
            order.setDescription(request.description());
        }
        if (request.amount() != null) {
            order.setAmount(request.amount());
        }
        if (request.status() != null) {
            order.setStatus(request.status());
        }
        order.setUpdatedAt(Instant.now());
        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional
    public void deleteOrder(UUID id, UUID tenantId) {
        OrderEntity order = orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new OrderNotFoundException(id));
        orderRepository.delete(order);
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(UUID id) {
            super("Order not found: " + id);
        }
    }
}
