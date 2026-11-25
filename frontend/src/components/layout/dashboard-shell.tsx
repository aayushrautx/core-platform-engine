"use client";

import type { ReactNode } from "react";
import { AuthProvider } from "@/components/providers/auth-provider";
import { AppSidebar } from "@/components/layout/app-sidebar";
import { TopBar } from "@/components/layout/top-bar";

export function DashboardShell({ children }: { children: ReactNode }) {
  return (
    <AuthProvider>
      <div className="flex h-screen overflow-hidden">
        <AppSidebar />
        <div className="flex flex-1 flex-col overflow-hidden">
          <TopBar />
          <main className="flex-1 overflow-y-auto p-6">{children}</main>
        </div>
      </div>
    </AuthProvider>
  );
}
