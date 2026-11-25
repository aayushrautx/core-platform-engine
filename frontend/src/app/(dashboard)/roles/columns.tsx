"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { AppFeatures } from "@/lib/table-features";
import type { RoleResponse } from "@/types/api";
import { Badge } from "@/components/ui/badge";
import { formatRelative } from "@/lib/utils";
import { ArrowUpDown } from "lucide-react";

export const roleColumns: ColumnDef<AppFeatures, RoleResponse>[] = [
  {
    accessorKey: "name",
    header: ({ column }) => (
      <button
        className="flex items-center gap-1"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Name
        <ArrowUpDown className="h-3 w-3" />
      </button>
    ),
  },
  {
    accessorKey: "description",
    header: "Description",
    cell: ({ row }) => row.getValue("description") || "—",
  },
  {
    accessorKey: "isSystem",
    header: "Type",
    cell: ({ row }) => (
      <Badge variant={row.getValue("isSystem") ? "default" : "outline"}>
        {row.getValue("isSystem") ? "System" : "Custom"}
      </Badge>
    ),
  },
  {
    accessorKey: "isActive",
    header: "Status",
    cell: ({ row }) => (
      <Badge variant={row.getValue("isActive") ? "default" : "secondary"}>
        {row.getValue("isActive") ? "Active" : "Inactive"}
      </Badge>
    ),
  },
  {
    accessorKey: "createdAt",
    header: "Created",
    cell: ({ row }) => formatRelative(row.getValue("createdAt")),
  },
];
