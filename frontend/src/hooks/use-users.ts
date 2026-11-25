"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPatch, apiPost, apiDelete } from "@/lib/api-client";
import { QUERY_KEYS, STALE_TIMES } from "@/lib/constants";
import type {
  UserResponse,
  UpdateUserRequest,
  AssignRoleRequest,
  RoleResponse,
} from "@/types/api";

export function useUsers(tenantId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.USERS(tenantId),
    queryFn: () => apiGet<UserResponse[]>(`/api/users?tenantId=${tenantId}`),
    staleTime: STALE_TIMES.LIST,
    enabled: !!tenantId,
  });
}

export function useUser(id: string) {
  return useQuery({
    queryKey: QUERY_KEYS.USER(id),
    queryFn: () => apiGet<UserResponse>(`/api/users/${id}`),
    staleTime: STALE_TIMES.DETAIL,
    enabled: !!id,
  });
}

export function useUserRoles(userId: string) {
  return useQuery({
    queryKey: QUERY_KEYS.USER_ROLES(userId),
    queryFn: () => apiGet<RoleResponse[]>(`/api/users/${userId}/roles`),
    staleTime: STALE_TIMES.DETAIL,
    enabled: !!userId,
  });
}

export function useUpdateUser(tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateUserRequest }) =>
      apiPatch<UserResponse>(`/api/users/${id}`, data),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.USER(id) });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.USERS(tenantId) });
    },
  });
}

export function useToggleUserActive(tenantId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      apiPatch<UserResponse>(`/api/users/${id}/${active ? "activate" : "deactivate"}`, {}),
    onSuccess: (_, { id }) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.USER(id) });
      qc.invalidateQueries({ queryKey: QUERY_KEYS.USERS(tenantId) });
    },
  });
}

export function useAssignRole(userId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: AssignRoleRequest) =>
      apiPost<void>(`/api/users/${userId}/roles`, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.USER_ROLES(userId) });
    },
  });
}

export function useUnassignRole(userId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (roleId: string) =>
      apiDelete(`/api/users/${userId}/roles/${roleId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.USER_ROLES(userId) });
    },
  });
}
