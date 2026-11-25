import { Suspense } from "react";
import { LoadingSpinner } from "@/components/ui/loading-spinner";
import { RoleDetail } from "./role-detail";

export default function RoleDetailPage({ params }: { params: Promise<{ id: string }> }) {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <RoleDetail params={params} />
    </Suspense>
  );
}
