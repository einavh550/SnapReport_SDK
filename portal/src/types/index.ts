// Types mirror the FastAPI backend response shapes.
// Auth + project responses use snake_case; portal report responses use camelCase.

export interface DeveloperResponse {
  id: string;
  email: string;
}

export interface TokenResponse {
  access_token: string;
  token_type: string;
}

export interface ProjectCreated {
  id: string;
  app_name: string;
  api_key: string; // raw key, shown only once
  api_key_prefix: string;
  created_at: string;
}

export interface Project {
  id: string;
  app_name: string;
  api_key_prefix: string;
  is_active: boolean;
  created_at: string;
}

export type TicketStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "ARCHIVED";

export const TICKET_STATUSES: TicketStatus[] = [
  "OPEN",
  "IN_PROGRESS",
  "RESOLVED",
  "ARCHIVED",
];

export interface TicketListItem {
  id: string;
  projectId: string;
  timestamp: string;
  status: TicketStatus;
  imageUrl: string | null;
  deviceSummary: string;
  appVersion: string | null;
  description: string | null;
}

export interface TicketDetail {
  id: string;
  projectId: string;
  timestamp: string;
  status: TicketStatus;
  imageUrl: string | null;
  description: string | null;
  userId: string | null;
  screenshotBlockedReason: string | null;
  deviceMetadata: Record<string, unknown>;
  appMetadata: Record<string, unknown>;
}

export interface TicketFilters {
  projectId?: string;
  status?: TicketStatus | "";
  osVersion?: string;
  appVersion?: string;
  deviceModel?: string;
  dateFrom?: string;
  dateTo?: string;
}
