"use client";

import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { LoadingSpinner } from "@/components/ui/loading-spinner";
import { useRolePermissions, useAssignPermission, useUnassignPermission } from "@/hooks/use-roles";
import { usePermissions } from "@/hooks/use-permissions";
import type { RoleResponse } from "@/types/api";
import { toast } from "sonner";
import { Plus, X } from "lucide-react";

interface RolePermissionsDialogProps {
  role: RoleResponse | null;
  tenantId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function RolePermissionsDialog({
  role,
  tenantId,
  open,
  onOpenChange,
}: RolePermissionsDialogProps) {
  const { data: rolePerms, isLoading } = useRolePermissions(role?.id ?? "");
  const { data: allPerms } = usePermissions();
  const assignPerm = useAssignPermission(role?.id ?? "", tenantId);
  const unassignPerm = useUnassignPermission(role?.id ?? "", tenantId);

  if (!role) return null;

  const assignedIds = new Set(rolePerms?.map((p) => p.id) ?? []);
  const availablePerms = allPerms?.filter((p) => !assignedIds.has(p.id)) ?? [];

  function handleAssign(permissionId: string) {
    assignPerm.mutate(
      { permissionId, scope: "*" },
      {
        onSuccess: () => toast.success("Permission assigned"),
        onError: (err) => toast.error(err.message),
      },
    );
  }

  function handleUnassign(permissionId: string) {
    unassignPerm.mutate(permissionId, {
      onSuccess: () => toast.success("Permission removed"),
      onError: (err) => toast.error(err.message),
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Permissions for {role.name}</DialogTitle>
        </DialogHeader>

        {isLoading ? (
          <LoadingSpinner />
        ) : (
          <div className="space-y-4">
            <div>
              <p className="mb-2 text-sm font-medium">Assigned Permissions</p>
              {rolePerms?.length ? (
                <div className="flex flex-wrap gap-2">
                  {rolePerms.map((perm) => (
                    <Badge key={perm.id} variant="secondary" className="gap-1">
                      {perm.resource}:{perm.action}
                      <button
                        onClick={() => handleUnassign(perm.id)}
                        className="ml-1 rounded-full hover:bg-muted"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">No permissions assigned</p>
              )}
            </div>

            {availablePerms.length > 0 && (
              <div>
                <p className="mb-2 text-sm font-medium">Available Permissions</p>
                <div className="max-h-48 overflow-y-auto">
                  <div className="flex flex-wrap gap-2">
                    {availablePerms.map((perm) => (
                      <Button
                        key={perm.id}
                        variant="outline"
                        size="sm"
                        onClick={() => handleAssign(perm.id)}
                        disabled={assignPerm.isPending}
                      >
                        <Plus className="mr-1 h-3 w-3" />
                        {perm.resource}:{perm.action}
                      </Button>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
