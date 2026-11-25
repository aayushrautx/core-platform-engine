"use client";

import { UserMenu } from "./user-menu";

export function TopBar() {
  return (
    <header className="flex h-14 items-center justify-between border-b bg-background px-6">
      <div />
      <UserMenu />
    </header>
  );
}
