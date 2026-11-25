"use client";

import { LogOut, User } from "lucide-react";
import { useAuth } from "@/components/providers/auth-provider";

export function UserMenu() {
  const { user, logout } = useAuth();

  if (!user) return null;

  const initials = user.fullName
    ? user.fullName
        .split(" ")
        .map((n) => n[0])
        .join("")
        .toUpperCase()
        .slice(0, 2)
    : user.preferredUsername?.slice(0, 2).toUpperCase() ?? "??";

  return (
    <div className="flex items-center gap-3">
      <div className="text-right hidden sm:block">
        <p className="text-sm font-medium leading-none">{user.fullName || user.preferredUsername}</p>
        <p className="text-xs text-muted-foreground">{user.email}</p>
      </div>
      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-xs font-medium text-primary-foreground">
        {initials}
      </div>
      <button
        onClick={logout}
        className="flex items-center gap-1 rounded-md px-2 py-1 text-sm text-muted-foreground hover:text-foreground transition-colors"
        title="Sign out"
      >
        <LogOut className="h-4 w-4" />
      </button>
    </div>
  );
}
