"use client";

import { use } from "react";
import { useRole, useRolePermissions } from "@/hooks/use-roles";
import { LoadingSpinner } from "@/components/ui/loading-spinner";
import { ErrorAlert } from "@/components/ui/error-alert";
import { PageHeader } from "@/components/layout/page-header";
import { Badge } from "@/components/ui/badge";

export function RoleDetail({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const { data: role, isLoading, error } = useRole(id);
  const { data: permissions } = useRolePermissions(id);

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorAlert message={error.message} />;
  if (!role) return null;

  return (
    <div className="space-y-6">
      <PageHeader
        title={role.name}
        description={role.description ?? undefined}
      />

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-lg border p-4">
          <p className="text-sm font-medium text-muted-foreground">Type</p>
          <Badge className="mt-1" variant={role.isSystem ? "default" : "outline"}>
            {role.isSystem ? "System" : "Custom"}
          </Badge>
        </div>
        <div className="rounded-lg border p-4">
          <p className="text-sm font-medium text-muted-foreground">Status</p>
          <Badge className="mt-1" variant={role.isActive ? "default" : "secondary"}>
            {role.isActive ? "Active" : "Inactive"}
          </Badge>
        </div>
      </div>

      <div>
        <h2 className="mb-3 text-lg font-medium">Permissions</h2>
        {permissions?.length ? (
          <div className="flex flex-wrap gap-2">
            {permissions.map((p) => (
              <Badge key={p.id} variant="secondary">
                {p.resource}:{p.action}
              </Badge>
            ))}
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">No permissions assigned</p>
        )}
      </div>
    </div>
  );
}
