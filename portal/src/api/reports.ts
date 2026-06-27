import { api } from "./client";
import type {
  TicketDetail,
  TicketFilters,
  TicketListItem,
  TicketStatus,
} from "../types";

export async function listReports(
  filters: TicketFilters = {},
): Promise<TicketListItem[]> {
  const params: Record<string, string> = {};
  if (filters.projectId) params.projectId = filters.projectId;
  if (filters.status) params.status = filters.status;
  if (filters.osVersion) params.osVersion = filters.osVersion;
  if (filters.appVersion) params.appVersion = filters.appVersion;
  if (filters.deviceModel) params.deviceModel = filters.deviceModel;
  if (filters.dateFrom) params.dateFrom = filters.dateFrom;
  if (filters.dateTo) params.dateTo = filters.dateTo;

  const { data } = await api.get<TicketListItem[]>("/api/portal/reports", {
    params,
  });
  return data;
}

export async function getReport(ticketId: string): Promise<TicketDetail> {
  const { data } = await api.get<TicketDetail>(
    `/api/portal/reports/${ticketId}`,
  );
  return data;
}

export async function updateReportStatus(
  ticketId: string,
  status: TicketStatus,
): Promise<TicketDetail> {
  const { data } = await api.patch<TicketDetail>(
    `/api/portal/reports/${ticketId}/status`,
    { status },
  );
  return data;
}
