"use client";

import type { ColumnDef } from "@tanstack/react-table";
import type { AppFeatures } from "@/lib/table-features";
import type { UserResponse } from "@/types/api";
import { Badge } from "@/components/ui/badge";
import { formatRelative } from "@/lib/utils";
import { ArrowUpDown } from "lucide-react";

export const userColumns: ColumnDef<AppFeatures, UserResponse>[] = [
  {
    accessorKey: "username",
    header: ({ column }) => (
      <button
        className="flex items-center gap-1"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Username
        <ArrowUpDown className="h-3 w-3" />
      </button>
    ),
  },
  {
    accessorKey: "email",
    header: "Email",
  },
  {
    accessorKey: "department",
    header: "Department",
    cell: ({ row }) => row.getValue("department") || "—",
  },
  {
    accessorKey: "designation",
    header: "Designation",
    cell: ({ row }) => row.getValue("designation") || "—",
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
