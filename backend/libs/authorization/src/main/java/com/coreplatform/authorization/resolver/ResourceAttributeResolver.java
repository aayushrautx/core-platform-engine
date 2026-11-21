package com.coreplatform.authorization.resolver;

import java.util.Map;

public interface ResourceAttributeResolver {

    String supportedResource();

    Map<String, Object> resolve(String resource, Object resourceId);
}
