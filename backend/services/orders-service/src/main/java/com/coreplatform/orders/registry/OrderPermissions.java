package com.coreplatform.orders.registry;

import com.coreplatform.authorization.registry.PermissionDeclaration;
import com.coreplatform.authorization.registry.PermissionRegistrar;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderPermissions implements PermissionRegistrar {

    @Override
    public List<PermissionDeclaration> declare() {
        return List.of(
                PermissionDeclaration.of("order", "create", "Create orders"),
                PermissionDeclaration.of("order", "read", "View orders"),
                PermissionDeclaration.of("order", "update", "Update orders"),
                PermissionDeclaration.of("order", "delete", "Delete orders")
        );
    }
}
