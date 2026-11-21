package com.coreplatform.authorization.event;

public interface AuthorizationChangePublisher {

    void publish(AuthorizationChangeEvent event);
}
