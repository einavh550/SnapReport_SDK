import { api } from "./client";
import type { DeveloperResponse, TokenResponse } from "../types";

export async function register(
  email: string,
  password: string,
): Promise<DeveloperResponse> {
  const { data } = await api.post<DeveloperResponse>("/api/auth/register", {
    email,
    password,
  });
  return data;
}

export async function login(
  email: string,
  password: string,
): Promise<TokenResponse> {
  const { data } = await api.post<TokenResponse>("/api/auth/login", {
    email,
    password,
  });
  return data;
}
