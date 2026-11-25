"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiPost } from "@/lib/api-client";
import { QUERY_KEYS, STALE_TIMES } from "@/lib/constants";
import type { PermissionResponse, CreatePermissionRequest } from "@/types/api";

export function usePermissions() {
  return useQuery({
    queryKey: QUERY_KEYS.PERMISSIONS,
    queryFn: () => apiGet<PermissionResponse[]>("/api/permissions"),
    staleTime: STALE_TIMES.LIST,
  });
}

export function useCreatePermission() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreatePermissionRequest) =>
      apiPost<PermissionResponse>("/api/permissions", data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.PERMISSIONS });
    },
  });
}
