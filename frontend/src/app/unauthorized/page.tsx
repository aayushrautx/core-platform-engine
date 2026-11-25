"use client";

import Link from "next/link";
import { ShieldAlert } from "lucide-react";

export default function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="mx-auto flex w-full max-w-sm flex-col items-center gap-6 px-4 text-center">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-destructive/10 text-destructive">
          <ShieldAlert className="h-6 w-6" />
        </div>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Access Denied
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            You do not have permission to access this resource. Contact your
            administrator if you believe this is an error.
          </p>
        </div>
        <Link
          href="/"
          className="flex h-10 items-center justify-center rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
        >
          Back to Dashboard
        </Link>
      </div>
    </div>
  );
}
