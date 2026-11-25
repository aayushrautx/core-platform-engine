"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { useCreateRole, useUpdateRole, useRoles } from "@/hooks/use-roles";
import type { RoleResponse, CreateRoleRequest } from "@/types/api";
import { toast } from "sonner";

interface RoleFormDialogProps {
  role: RoleResponse | null;
  tenantId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function RoleFormDialog({
  role,
  tenantId,
  open,
  onOpenChange,
}: RoleFormDialogProps) {
  const createRole = useCreateRole(tenantId);
  const updateRole = useUpdateRole(tenantId);
  const { data: allRoles } = useRoles(tenantId);
  const isEdit = !!role;

  const { register, handleSubmit, reset } = useForm<CreateRoleRequest>();

  useEffect(() => {
    if (role) {
      reset({
        name: role.name,
        description: role.description ?? "",
        parentRoleId: role.parentRoleId ?? undefined,
      });
    } else {
      reset({ name: "", description: "", parentRoleId: undefined });
    }
  }, [role, reset, open]);

  function onSubmit(data: CreateRoleRequest) {
    if (isEdit) {
      updateRole.mutate(
        { id: role!.id, data: { name: data.name, description: data.description, parentRoleId: data.parentRoleId } },
        {
          onSuccess: () => {
            toast.success("Role updated");
            onOpenChange(false);
          },
          onError: (err) => toast.error(err.message),
        },
      );
    } else {
      createRole.mutate(data, {
        onSuccess: () => {
          toast.success("Role created");
          onOpenChange(false);
        },
        onError: (err) => toast.error(err.message),
      });
    }
  }

  const isPending = createRole.isPending || updateRole.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{isEdit ? "Edit Role" : "Create Role"}</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="name">Name</Label>
            <Input id="name" {...register("name", { required: true })} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea id="description" rows={3} {...register("description")} />
          </div>
          {!isEdit && (
            <div className="space-y-2">
              <Label htmlFor="parentRoleId">Parent Role</Label>
              <select
                id="parentRoleId"
                className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                {...register("parentRoleId", { required: true })}
              >
                <option value="">Select a parent role...</option>
                {allRoles?.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </div>
          )}
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending ? "Saving..." : isEdit ? "Save Changes" : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
