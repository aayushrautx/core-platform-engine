"use client";

import { Shield } from "lucide-react";
import { BFF_URL } from "@/lib/constants";

export default function LoginPage() {
  function handleLogin() {
    window.location.href = `${BFF_URL}/oauth2/authorization/keycloak`;
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="mx-auto flex w-full max-w-sm flex-col items-center gap-8 px-4">
        <div className="flex flex-col items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-primary text-primary-foreground">
            <Shield className="h-6 w-6" />
          </div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Core Platform
          </h1>
          <p className="text-sm text-muted-foreground text-center">
            Sign in to access the authorization management console
          </p>
        </div>

        <button
          onClick={handleLogin}
          className="flex h-11 w-full items-center justify-center rounded-lg bg-primary px-6 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
        >
          Sign in with SSO
        </button>

        <p className="text-xs text-muted-foreground">
          Authenticates via Keycloak through the BFF gateway
        </p>
      </div>
    </div>
  );
}
