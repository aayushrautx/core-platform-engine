"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost, apiPut, apiDelete } from "@/lib/api-client";
import { QUERY_KEYS, STALE_TIMES } from "@/lib/constants";
import type {
  RoleResponse,
  CreateRoleRequest,
  UpdateRoleRequest,
  PermissionResponse,
  RolePermissionRequest,
} from "@/types/api";

export function useRoles(tenantId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.ROLES(tenantId),
    queryFn: () => apiGet<RoleResponse[]>(`/api/roles?tenantId=${tenantId}`),
    staleTime: STALE_TIMES.LIST,
    enabled: !!tenantId,
  });
}

export function useRole(id: string) {
  return useQuery({
    queryKey: QUERY_KEYS.ROLE(id),
    queryFn: () => apiGet<RoleResponse>(`/api/roles/${id}`),
    staleTime: STALE_TIMES.DETAIL,
    enabled: !!id,
  });
}

export function useRolePermissions(roleId: string) {
  return useQuery({
    queryKey: ["role-permissions", roleId],
    queryFn: () => apiGet<PermissionResponse[]>(`/api/roles/${roleId}/permissions`),
    staleTime: STALE_TIMES.DETAIL,
    enabled: !!roleId,
  });
}

export function useCreateRole(tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateRoleRequest) =>
      apiPost<RoleResponse>(`/api/roles?tenantId=${tenantId}`, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.ROLES(tenantId) });
    },
  });
}

export function useUpdateRole(tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateRoleRequest }) =>
      apiPut<RoleResponse>(`/api/roles/${id}`, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.ROLE(id) });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.ROLES(tenantId) });
    },
  });
}

export function useDeleteRole(tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => apiDelete(`/api/roles/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.ROLES(tenantId) });
    },
  });
}

export function useAssignPermission(roleId: string, tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: RolePermissionRequest) =>
      apiPost<void>(`/api/roles/${roleId}/permissions`, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["role-permissions", roleId] });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.ROLES(tenantId) });
    },
  });
}

export function useUnassignPermission(roleId: string, tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (permissionId: string) =>
      apiDelete(`/api/roles/${roleId}/permissions/${permissionId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["role-permissions", roleId] });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.ROLES(tenantId) });
    },
  });
}
