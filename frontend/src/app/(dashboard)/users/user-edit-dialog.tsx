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
import { Button } from "@/components/ui/button";
import { useUpdateUser, useToggleUserActive } from "@/hooks/use-users";
import type { UserResponse, UpdateUserRequest } from "@/types/api";
import { toast } from "sonner";

interface UserEditDialogProps {
  user: UserResponse | null;
  tenantId: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function UserEditDialog({
  user,
  tenantId,
  open,
  onOpenChange,
}: UserEditDialogProps) {
  const updateUser = useUpdateUser(tenantId);
  const toggleActive = useToggleUserActive(tenantId);

  const { register, handleSubmit, reset } = useForm<UpdateUserRequest>();

  useEffect(() => {
    if (user) {
      reset({
        username: user.username,
        email: user.email,
        orgId: user.orgId ?? "",
        department: user.department ?? "",
        designation: user.designation ?? "",
      });
    }
  }, [user, reset]);

  function onSubmit(data: UpdateUserRequest) {
    if (!user) return;
    updateUser.mutate(
      { id: user.id, data },
      {
        onSuccess: () => {
          toast.success("User updated");
          onOpenChange(false);
        },
        onError: (err) => toast.error(err.message),
      },
    );
  }

  function handleToggleActive() {
    if (!user) return;
    toggleActive.mutate(
      { id: user.id, active: !user.isActive },
      {
        onSuccess: () => {
          toast.success(user.isActive ? "User deactivated" : "User activated");
          onOpenChange(false);
        },
        onError: (err) => toast.error(err.message),
      },
    );
  }

  if (!user) return null;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Edit User</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="username">Username</Label>
            <Input id="username" {...register("username")} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" {...register("email")} />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="department">Department</Label>
              <Input id="department" {...register("department")} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="designation">Designation</Label>
              <Input id="designation" {...register("designation")} />
            </div>
          </div>
          <div className="space-y-2">
            <Label htmlFor="orgId">Org ID</Label>
            <Input id="orgId" {...register("orgId")} />
          </div>
          <DialogFooter className="gap-2 sm:gap-0">
            <Button
              type="button"
              variant="outline"
              onClick={handleToggleActive}
              disabled={toggleActive.isPending}
            >
              {user.isActive ? "Deactivate" : "Activate"}
            </Button>
            <Button type="submit" disabled={updateUser.isPending}>
              {updateUser.isPending ? "Saving..." : "Save Changes"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
