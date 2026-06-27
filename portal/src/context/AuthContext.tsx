import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import * as authApi from "../api/auth";
import { getToken, setToken } from "../api/client";

interface AuthContextValue {
  token: string | null;
  email: string | null;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const EMAIL_KEY = "snapreport_email";

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setTokenState] = useState<string | null>(() => getToken());
  const [email, setEmail] = useState<string | null>(() =>
    localStorage.getItem(EMAIL_KEY),
  );

  const login = useCallback(async (emailArg: string, password: string) => {
    const res = await authApi.login(emailArg, password);
    setToken(res.access_token);
    setTokenState(res.access_token);
    localStorage.setItem(EMAIL_KEY, emailArg);
    setEmail(emailArg);
  }, []);

  const register = useCallback(async (emailArg: string, password: string) => {
    await authApi.register(emailArg, password);
    // Auto-login after successful registration.
    const res = await authApi.login(emailArg, password);
    setToken(res.access_token);
    setTokenState(res.access_token);
    localStorage.setItem(EMAIL_KEY, emailArg);
    setEmail(emailArg);
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setTokenState(null);
    localStorage.removeItem(EMAIL_KEY);
    setEmail(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      token,
      email,
      isAuthenticated: Boolean(token),
      login,
      register,
      logout,
    }),
    [token, email, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}
