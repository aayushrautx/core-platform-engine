"use client";

import { useState } from "react";
import type { ColumnDef } from "@tanstack/react-table";
import type { AppFeatures } from "@/lib/table-features";
import { PageHeader } from "@/components/layout/page-header";
import { DataTable } from "@/components/ui/data-table";
import { LoadingSpinner } from "@/components/ui/loading-spinner";
import { ErrorAlert } from "@/components/ui/error-alert";
import { Button } from "@/components/ui/button";
import { usePermissions } from "@/hooks/use-permissions";
import { PermissionFormDialog } from "./permission-form-dialog";
import type { PermissionResponse } from "@/types/api";
import { Plus } from "lucide-react";

const permissionColumns: ColumnDef<AppFeatures, PermissionResponse>[] = [
  {
    accessorKey: "resource",
    header: "Resource",
  },
  {
    accessorKey: "action",
    header: "Action",
  },
  {
    accessorKey: "description",
    header: "Description",
    cell: ({ row }) => row.getValue("description") || "—",
  },
  {
    accessorKey: "serviceName",
    header: "Service",
    cell: ({ row }) => row.getValue("serviceName") || "—",
  },
];

export default function PermissionsPage() {
  const { data: permissions, isLoading, error } = usePermissions();
  const [createOpen, setCreateOpen] = useState(false);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Permissions"
        description="Define fine-grained permissions for resources and actions"
        actions={
          <Button onClick={() => setCreateOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Create Permission
          </Button>
        }
      />

      {isLoading && <LoadingSpinner />}
      {error && <ErrorAlert message={error.message} />}
      {permissions && (
        <DataTable
          columns={permissionColumns}
          data={permissions}
          searchKey="resource"
          searchPlaceholder="Search by resource..."
        />
      )}

      <PermissionFormDialog open={createOpen} onOpenChange={setCreateOpen} />
    </div>
  );
}
