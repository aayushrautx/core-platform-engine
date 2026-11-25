"use client";

import { useState } from "react";
import { PageHeader } from "@/components/layout/page-header";
import { DataTable } from "@/components/ui/data-table";
import { LoadingSpinner } from "@/components/ui/loading-spinner";
import { ErrorAlert } from "@/components/ui/error-alert";
import { Button } from "@/components/ui/button";
import { useRoles, useDeleteRole } from "@/hooks/use-roles";
import { roleColumns } from "./columns";
import { RoleFormDialog } from "./role-form-dialog";
import { RolePermissionsDialog } from "./role-permissions-dialog";
import type { RoleResponse } from "@/types/api";
import type { ColumnDef } from "@tanstack/react-table";
import type { AppFeatures } from "@/lib/table-features";
import { DEFAULT_TENANT_ID } from "@/lib/constants";
import { toast } from "sonner";
import { KeyRound, Pencil, Plus, Trash2 } from "lucide-react";

export default function RolesPage() {
  const { data: roles, isLoading, error } = useRoles(DEFAULT_TENANT_ID);
  const deleteRole = useDeleteRole(DEFAULT_TENANT_ID);
  const [formRole, setFormRole] = useState<RoleResponse | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [permsRole, setPermsRole] = useState<RoleResponse | null>(null);

  function handleDelete(role: RoleResponse) {
    if (role.isSystem) {
      toast.error("System roles cannot be deleted");
      return;
    }
    deleteRole.mutate(role.id, {
      onSuccess: () => toast.success("Role deleted"),
      onError: (err) => toast.error(err.message),
    });
  }

  const columns: ColumnDef<AppFeatures, RoleResponse>[] = [
    ...roleColumns,
    {
      id: "actions",
      header: "",
      cell: ({ row }) => (
        <div className="flex items-center gap-1">
          {!row.original.isSystem && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => {
                setFormRole(row.original);
                setFormOpen(true);
              }}
            >
              <Pencil className="h-3.5 w-3.5" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setPermsRole(row.original)}
          >
            <KeyRound className="h-3.5 w-3.5" />
          </Button>
          {!row.original.isSystem && (
            <Button
              variant="ghost"
              size="sm"
              onClick={() => handleDelete(row.original)}
            >
              <Trash2 className="h-3.5 w-3.5 text-destructive" />
            </Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Roles"
        description="Create and manage roles with permission assignments"
        actions={
          <Button
            onClick={() => {
              setFormRole(null);
              setFormOpen(true);
            }}
          >
            <Plus className="mr-2 h-4 w-4" />
            Create Role
          </Button>
        }
      />

      {isLoading && <LoadingSpinner />}
      {error && <ErrorAlert message={error.message} />}
      {roles && (
        <DataTable
          columns={columns}
          data={roles}
          searchKey="name"
          searchPlaceholder="Search roles..."
        />
      )}

      <RoleFormDialog
        role={formRole}
        tenantId={DEFAULT_TENANT_ID}
        open={formOpen}
        onOpenChange={setFormOpen}
      />
      <RolePermissionsDialog
        role={permsRole}
        tenantId={DEFAULT_TENANT_ID}
        open={!!permsRole}
        onOpenChange={(open) => !open && setPermsRole(null)}
      />
    </div>
  );
}
