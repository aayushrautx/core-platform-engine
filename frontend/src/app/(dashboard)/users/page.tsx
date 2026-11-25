"use client";

import { useState } from "react";
import { PageHeader } from "@/components/layout/page-header";
import { DataTable } from "@/components/ui/data-table";
import { LoadingSpinner } from "@/components/ui/loading-spinner";
import { ErrorAlert } from "@/components/ui/error-alert";
import { Button } from "@/components/ui/button";
import { useUsers } from "@/hooks/use-users";
import { useAuth } from "@/components/providers/auth-provider";
import { userColumns } from "./columns";
import { UserEditDialog } from "./user-edit-dialog";
import { UserRolesDialog } from "./user-roles-dialog";
import type { UserResponse } from "@/types/api";
import type { ColumnDef } from "@tanstack/react-table";
import type { AppFeatures } from "@/lib/table-features";
import { DEFAULT_TENANT_ID } from "@/lib/constants";
import { Pencil, Shield } from "lucide-react";

export default function UsersPage() {
  const { data: users, isLoading, error } = useUsers(DEFAULT_TENANT_ID);
  const [editUser, setEditUser] = useState<UserResponse | null>(null);
  const [rolesUser, setRolesUser] = useState<UserResponse | null>(null);

  const columns: ColumnDef<AppFeatures, UserResponse>[] = [
    ...userColumns,
    {
      id: "actions",
      header: "",
      cell: ({ row }) => (
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setEditUser(row.original)}
          >
            <Pencil className="h-3.5 w-3.5" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setRolesUser(row.original)}
          >
            <Shield className="h-3.5 w-3.5" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Users"
        description="Manage user accounts and their role assignments"
      />

      {isLoading && <LoadingSpinner />}
      {error && <ErrorAlert message={error.message} />}
      {users && (
        <DataTable
          columns={columns}
          data={users}
          searchKey="username"
          searchPlaceholder="Search users..."
        />
      )}

      <UserEditDialog
        user={editUser}
        tenantId={DEFAULT_TENANT_ID}
        open={!!editUser}
        onOpenChange={(open) => !open && setEditUser(null)}
      />
      <UserRolesDialog
        user={rolesUser}
        tenantId={DEFAULT_TENANT_ID}
        open={!!rolesUser}
        onOpenChange={(open) => !open && setRolesUser(null)}
      />
    </div>
  );
}
