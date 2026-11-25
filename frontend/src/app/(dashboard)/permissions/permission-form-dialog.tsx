"use client";

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
import { useCreatePermission } from "@/hooks/use-permissions";
import type { CreatePermissionRequest } from "@/types/api";
import { toast } from "sonner";

interface PermissionFormDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function PermissionFormDialog({
  open,
  onOpenChange,
}: PermissionFormDialogProps) {
  const createPermission = useCreatePermission();
  const { register, handleSubmit, reset } = useForm<CreatePermissionRequest>();

  function onSubmit(data: CreatePermissionRequest) {
    createPermission.mutate(data, {
      onSuccess: () => {
        toast.success("Permission created");
        reset();
        onOpenChange(false);
      },
      onError: (err) => toast.error(err.message),
    });
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Create Permission</DialogTitle>
        </DialogHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="resource">Resource</Label>
            <Input
              id="resource"
              placeholder="e.g. orders, users"
              {...register("resource", { required: true })}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="action">Action</Label>
            <Input
              id="action"
              placeholder="e.g. read, write, delete"
              {...register("action", { required: true })}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              rows={2}
              {...register("description")}
            />
          </div>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
            >
              Cancel
            </Button>
            <Button type="submit" disabled={createPermission.isPending}>
              {createPermission.isPending ? "Creating..." : "Create"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
