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
import { useUserRoles, useAssignRole, useUnassignRole } from "@/hooks/use-users";
import { useRoles } from "@/hooks/use-roles";
import type { UserResponse } from "@/types/api";
import { toast } from "sonner";
import { Plus, X } from "lucide-react";

interface UserRolesDialogProps {
  user: UserResponse | null;
  tenantId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function UserRolesDialog({
  user,
  tenantId,
  open,
  onOpenChange,
}: UserRolesDialogProps) {
  const { data: userRoles, isLoading: rolesLoading } = useUserRoles(user?.id ?? "");
  const { data: allRoles } = useRoles(tenantId);
  const assignRole = useAssignRole(user?.id ?? "");
  const unassignRole = useUnassignRole(user?.id ?? "");

  if (!user) return null;

  const assignedIds = new Set(userRoles?.map((r) => r.id) ?? []);
  const availableRoles = allRoles?.filter((r) => !assignedIds.has(r.id)) ?? [];

  function handleAssign(roleId: string) {
    assignRole.mutate(
      { roleId },
      {
        onSuccess: () => toast.success("Role assigned"),
        onError: (err) => toast.error(err.message),
      },
    );
  }

  function handleUnassign(roleId: string) {
    unassignRole.mutate(roleId, {
      onSuccess: () => toast.success("Role removed"),
      onError: (err) => toast.error(err.message),
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Roles for {user.username}</DialogTitle>
        </DialogHeader>

        {rolesLoading ? (
          <LoadingSpinner />
        ) : (
          <div className="space-y-4">
            <div>
              <p className="mb-2 text-sm font-medium">Assigned Roles</p>
              {userRoles?.length ? (
                <div className="flex flex-wrap gap-2">
                  {userRoles.map((role) => (
                    <Badge key={role.id} variant="secondary" className="gap-1">
                      {role.name}
                      <button
                        onClick={() => handleUnassign(role.id)}
                        className="ml-1 rounded-full hover:bg-muted"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-muted-foreground">No roles assigned</p>
              )}
            </div>

            {availableRoles.length > 0 && (
              <div>
                <p className="mb-2 text-sm font-medium">Available Roles</p>
                <div className="flex flex-wrap gap-2">
                  {availableRoles.map((role) => (
                    <Button
                      key={role.id}
                      variant="outline"
                      size="sm"
                      onClick={() => handleAssign(role.id)}
                      disabled={assignRole.isPending}
                    >
                      <Plus className="mr-1 h-3 w-3" />
                      {role.name}
                    </Button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </DialogContent>
    </Dialog>
  );
}
