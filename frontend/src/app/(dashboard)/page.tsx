"use client";

import { useAuth } from "@/components/providers/auth-provider";
import { PageHeader } from "@/components/layout/page-header";
import {
  Users,
  Shield,
  KeyRound,
  FileText,
  Bot,
  ShoppingCart,
} from "lucide-react";
import Link from "next/link";
import { ROUTES } from "@/lib/constants";

const QUICK_LINKS = [
  { title: "Users", description: "Manage user accounts", href: ROUTES.USERS, icon: Users },
  { title: "Roles", description: "Configure roles", href: ROUTES.ROLES, icon: Shield },
  { title: "Permissions", description: "Define permissions", href: ROUTES.PERMISSIONS, icon: KeyRound },
  { title: "Policies", description: "Authorization policies", href: ROUTES.POLICIES, icon: FileText },
  { title: "Service Accounts", description: "API credentials", href: ROUTES.SERVICE_ACCOUNTS, icon: Bot },
  { title: "Orders", description: "View orders", href: ROUTES.ORDERS, icon: ShoppingCart },
];

export default function DashboardPage() {
  const { user } = useAuth();

  return (
    <div className="space-y-8">
      <PageHeader
        title={`Welcome${user?.fullName ? `, ${user.fullName}` : ""}`}
        description="Core Platform Authorization Management Console"
      />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {QUICK_LINKS.map((link) => (
          <Link
            key={link.href}
            href={link.href}
            className="group flex items-start gap-4 rounded-lg border bg-card p-5 transition-colors hover:bg-accent"
          >
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
              <link.icon className="h-5 w-5" />
            </div>
            <div>
              <p className="font-medium">{link.title}</p>
              <p className="text-sm text-muted-foreground">
                {link.description}
              </p>
            </div>
          </Link>
        ))}
      </div>
    </div>
  );
}
