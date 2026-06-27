import { api } from "./client";
import type { Project, ProjectCreated } from "../types";

export async function listProjects(): Promise<Project[]> {
  const { data } = await api.get<Project[]>("/api/projects");
  return data;
}

export async function createProject(appName: string): Promise<ProjectCreated> {
  const { data } = await api.post<ProjectCreated>("/api/projects", {
    app_name: appName,
  });
  return data;
}

export async function regenerateApiKey(
  projectId: string,
): Promise<ProjectCreated> {
  const { data } = await api.post<ProjectCreated>(
    `/api/projects/${projectId}/regenerate-key`,
  );
  return data;
}
