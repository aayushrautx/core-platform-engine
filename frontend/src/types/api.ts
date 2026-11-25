// --- Users ---

export interface UserResponse {
  id: string;
  keycloakSub: string;
  username: string;
  email: string;
  tenantId: string;
  orgId: string | null;
  department: string | null;
  designation: string | null;
  isActive: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  orgId?: string;
  department?: string;
  designation?: string;
}

// --- Roles ---

export interface RoleResponse {
  id: string;
  tenantId: string;
  name: string;
  description: string | null;
  parentRoleId: string | null;
  isSystem: boolean;
  isActive: boolean;
  version: number;
  createdAt: string;
}

export interface CreateRoleRequest {
  name: string;
  description?: string;
  parentRoleId?: string;
}

export interface UpdateRoleRequest {
  name?: string;
  description?: string;
  parentRoleId?: string;
}

// --- Permissions ---

export interface PermissionResponse {
  id: string;
  resource: string;
  action: string;
  description: string | null;
  serviceName: string | null;
}

export interface CreatePermissionRequest {
  resource: string;
  action: string;
  description?: string;
}

export interface AssignRoleRequest {
  roleId: string;
}

export interface RolePermissionRequest {
  permissionId: string;
  scope: string;
}

// --- Service Accounts ---

export interface ServiceAccountResponse {
  id: string;
  keycloakSub: string;
  name: string;
  tenantId: string;
  isActive: boolean;
  version: number;
  createdAt: string;
}

export interface CreateServiceAccountRequest {
  keycloakSub: string;
  name: string;
  tenantId: string;
}

// --- Policy Sets ---

export interface PolicySetResponse {
  id: string;
  tenantId: string;
  name: string;
  description: string | null;
  targetResource: string;
  targetAction: string;
  combiningStrategy: string;
  isActive: boolean;
  version: number;
  policies: PolicyResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface CreatePolicySetRequest {
  name: string;
  description?: string;
  targetResource: string;
  targetAction: string;
  combiningStrategy: string;
}

export interface UpdatePolicySetRequest {
  description?: string;
  combiningStrategy?: string;
  isActive?: boolean;
}

// --- Policies ---

export interface PolicyResponse {
  id: string;
  policySetId: string;
  name: string;
  description: string | null;
  effect: "PERMIT" | "DENY";
  overrideRbac: boolean;
  conditionsJson: string | null;
  obligationsJson: string | null;
  priority: number;
  isActive: boolean;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePolicyRequest {
  name: string;
  description?: string;
  effect: "PERMIT" | "DENY";
  overrideRbac: boolean;
  conditionsJson: string;
  obligationsJson?: string;
  priority: number;
}

export interface UpdatePolicyRequest {
  description?: string;
  conditionsJson?: string;
  obligationsJson?: string;
  priority?: number;
  isActive?: boolean;
}

// --- Simulation ---

export interface SimulationRequest {
  tenantId: string;
  userKeycloakSub: string;
  resource: string;
  action: string;
  resourceAttributes?: Record<string, unknown>;
  subjectAttributes?: Record<string, unknown>;
  environmentAttributes?: Record<string, unknown>;
}

export interface PolicyEvaluationDetail {
  name: string;
  effect: string;
  overrideRbac: boolean;
  matched: boolean;
  missingAttributes: string[];
}

export interface SimulationResponse {
  allowed: boolean;
  rbacResult: boolean;
  scopeResult: string;
  tenantCheckResult: string;
  policiesEvaluated: PolicyEvaluationDetail[];
  matchedPolicy: string | null;
  finalReason: string;
  obligations: Record<string, unknown>[];
}

// --- Emergency Access ---

export interface EmergencyAccessRequest {
  grantedTo: string;
  reason: string;
  permissions: string[];
  durationMinutes: number;
}

export interface EmergencyAccessResponse {
  id: string;
  tenantId: string;
  grantedTo: string;
  grantedBy: string;
  reason: string;
  permissions: string;
  expiresAt: string;
  revokedAt: string | null;
  createdAt: string;
}

// --- Orders ---

export interface OrderResponse {
  id: string;
  tenantId: string;
  ownerId: string;
  ownerKeycloakSub: string;
  department: string | null;
  description: string;
  amount: number;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateOrderRequest {
  description: string;
  amount: number;
}

export interface UpdateOrderRequest {
  description?: string;
  amount?: number;
  status?: string;
}

// --- Paginated ---

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
