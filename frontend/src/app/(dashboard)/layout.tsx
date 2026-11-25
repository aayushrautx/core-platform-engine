import { Suspense } from "react";
import { DashboardShell } from "@/components/layout/dashboard-shell";
import { LoadingSpinner } from "@/components/ui/loading-spinner";

export default function DashboardLayout({ children }: LayoutProps<"/">) {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <DashboardShell>{children}</DashboardShell>
    </Suspense>
  );
}
