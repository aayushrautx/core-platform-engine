export interface AuthUser {
  subject: string;
  preferredUsername: string;
  fullName: string;
  email: string;
  emailVerified: boolean;
  issuedAt: string;
  expiresAt: string;
  issuer: string;
}

export interface AuthContextType {
  user: AuthUser | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  logout: () => void;
}
