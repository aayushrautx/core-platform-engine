rootProject.name = "backend"

include(
    "libs:authorization",
    "services:bff-gateway",
    "services:users-service",
    "services:orders-service",
)