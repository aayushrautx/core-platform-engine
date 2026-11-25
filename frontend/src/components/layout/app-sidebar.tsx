"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Users,
  Shield,
  KeyRound,
  FileText,
  Bot,
  AlertTriangle,
  ShoppingCart,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { ROUTES } from "@/lib/constants";

const NAV_SECTIONS = [
  {
    title: "Overview",
    items: [
      { title: "Dashboard", href: ROUTES.HOME, icon: LayoutDashboard },
    ],
  },
  {
    title: "Management",
    items: [
      { title: "Users", href: ROUTES.USERS, icon: Users },
      { title: "Roles", href: ROUTES.ROLES, icon: Shield },
      { title: "Permissions", href: ROUTES.PERMISSIONS, icon: KeyRound },
      { title: "Policies", href: ROUTES.POLICIES, icon: FileText },
      { title: "Service Accounts", href: ROUTES.SERVICE_ACCOUNTS, icon: Bot },
    ],
  },
  {
    title: "Security",
    items: [
      { title: "Emergency Access", href: ROUTES.EMERGENCY_ACCESS, icon: AlertTriangle },
    ],
  },
  {
    title: "Business",
    items: [
      { title: "Orders", href: ROUTES.ORDERS, icon: ShoppingCart },
    ],
  },
];

export function AppSidebar() {
  const pathname = usePathname();

  function isActive(href: string) {
    if (href === "/") return pathname === "/";
    return pathname.startsWith(href);
  }

  return (
    <aside className="flex h-full w-64 flex-col border-r bg-background">
      <div className="flex h-14 items-center border-b px-6">
        <Link href="/" className="flex items-center gap-2 font-semibold">
          <Shield className="h-5 w-5" />
          <span>Core Platform</span>
        </Link>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4">
        {NAV_SECTIONS.map((section) => (
          <div key={section.title} className="mb-6">
            <p className="mb-2 px-3 text-xs font-medium uppercase tracking-wider text-muted-foreground">
              {section.title}
            </p>
            <ul className="space-y-1">
              {section.items.map((item) => (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    className={cn(
                      "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
                      isActive(item.href)
                        ? "bg-accent text-accent-foreground"
                        : "text-muted-foreground hover:bg-accent hover:text-accent-foreground",
                    )}
                  >
                    <item.icon className="h-4 w-4" />
                    {item.title}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </nav>
    </aside>
  );
}
