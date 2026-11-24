package com.coreplatform.orders.controller;

import com.coreplatform.authorization.annotation.Authorize;
import com.coreplatform.authorization.model.FreshnessLevel;
import com.coreplatform.orders.dto.CreateOrderRequest;
import com.coreplatform.orders.dto.OrderResponse;
import com.coreplatform.orders.dto.UpdateOrderRequest;
import com.coreplatform.orders.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Authorize(resource = "order", action = "create")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request,
                                                      @RequestParam UUID tenantId,
                                                      @AuthenticationPrincipal Jwt jwt) {
        UUID ownerId = UUID.randomUUID();
        String sub = jwt.getSubject();
        String department = jwt.getClaimAsString("department");

        OrderResponse order = orderService.createOrder(request, tenantId, ownerId, sub, department);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    @GetMapping
    @Authorize(resource = "order", action = "read")
    public ResponseEntity<Page<OrderResponse>> listOrders(@RequestParam UUID tenantId,
                                                           Pageable pageable) {
        return ResponseEntity.ok(orderService.listOrders(tenantId, pageable));
    }

    @GetMapping("/mine")
    @Authorize(resource = "order", action = "read")
    public ResponseEntity<Page<OrderResponse>> listMyOrders(@RequestParam UUID tenantId,
                                                             @AuthenticationPrincipal Jwt jwt,
                                                             Pageable pageable) {
        return ResponseEntity.ok(orderService.listOwnOrders(tenantId, jwt.getSubject(), pageable));
    }

    @GetMapping("/{id}")
    @Authorize(resource = "order", action = "read", resourceIdParam = "id")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id,
                                                   @RequestParam UUID tenantId) {
        return ResponseEntity.ok(orderService.getOrder(id, tenantId));
    }

    @PutMapping("/{id}")
    @Authorize(resource = "order", action = "update", resourceIdParam = "id",
               freshness = FreshnessLevel.MEDIUM)
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable UUID id,
                                                      @RequestParam UUID tenantId,
                                                      @Valid @RequestBody UpdateOrderRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, tenantId, request));
    }

    @DeleteMapping("/{id}")
    @Authorize(resource = "order", action = "delete", resourceIdParam = "id",
               freshness = FreshnessLevel.HIGH)
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID id,
                                             @RequestParam UUID tenantId) {
        orderService.deleteOrder(id, tenantId);
        return ResponseEntity.noContent().build();
    }
}
