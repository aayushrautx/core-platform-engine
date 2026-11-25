export const ROUTES = {
  HOME: "/",
  LOGIN: "/login",
  UNAUTHORIZED: "/unauthorized",
  USERS: "/users",
  USER_DETAIL: (id: string) => `/users/${id}`,
  ROLES: "/roles",
  ROLE_NEW: "/roles/new",
  ROLE_DETAIL: (id: string) => `/roles/${id}`,
  PERMISSIONS: "/permissions",
  POLICIES: "/policies",
  POLICY_SET_DETAIL: (id: string) => `/policies/${id}`,
  POLICY_NEW: (policySetId: string) => `/policies/${policySetId}/new`,
  SIMULATE: "/policies/simulate",
  SERVICE_ACCOUNTS: "/service-accounts",
  SERVICE_ACCOUNT_DETAIL: (id: string) => `/service-accounts/${id}`,
  EMERGENCY_ACCESS: "/emergency-access",
  ORDERS: "/orders",
  ORDER_DETAIL: (id: string) => `/orders/${id}`,
} as const;

export const QUERY_KEYS = {
  AUTH_USER: ["auth", "user"] as const,
  USERS: (tenantId: string) => ["users", tenantId] as const,
  USER: (id: string) => ["user", id] as const,
  USER_ROLES: (userId: string) => ["user-roles", userId] as const,
  ROLES: (tenantId: string) => ["roles", tenantId] as const,
  ROLE: (id: string) => ["role", id] as const,
  PERMISSIONS: ["permissions"] as const,
  POLICY_SETS: (tenantId: string) => ["policy-sets", tenantId] as const,
  POLICY_SET: (id: string) => ["policy-set", id] as const,
  POLICIES: (policySetId: string) => ["policies", policySetId] as const,
  SERVICE_ACCOUNTS: (tenantId: string) => ["service-accounts", tenantId] as const,
  EMERGENCY_ACCESSES: (tenantId: string) => ["emergency-accesses", tenantId] as const,
  ORDERS: (tenantId: string, page: number) => ["orders", tenantId, page] as const,
  ORDER: (id: string) => ["order", id] as const,
} as const;

export const STALE_TIMES = {
  LIST: 5 * 60 * 1000,
  DETAIL: 30 * 1000,
  AUTH: 10 * 60 * 1000,
} as const;

export const BFF_URL = process.env.NEXT_PUBLIC_BFF_URL || "http://localhost:9010";

export const DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";
