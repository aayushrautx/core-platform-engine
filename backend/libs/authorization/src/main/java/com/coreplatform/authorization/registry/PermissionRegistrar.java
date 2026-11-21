package com.coreplatform.authorization.registry;

import java.util.List;

public interface PermissionRegistrar {

    List<PermissionDeclaration> declare();
}
