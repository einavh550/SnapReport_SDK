import { useEffect, useState, type FormEvent } from "react";
import { errorMessage } from "../api/client";
import {
  createProject,
  listProjects,
  regenerateApiKey,
} from "../api/projects";
import type { Project, ProjectCreated } from "../types";

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString();
}

export default function Projects() {
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [appName, setAppName] = useState("");
  const [creating, setCreating] = useState(false);

  // The raw key returned once (on create or regenerate).
  const [revealedKey, setRevealedKey] = useState<ProjectCreated | null>(null);
  const [copied, setCopied] = useState(false);

  const load = async () => {
    setLoading(true);
    try {
      setProjects(await listProjects());
    } catch (err) {
      setError(errorMessage(err, "Could not load projects."));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void load();
  }, []);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (!appName.trim()) return;
    setCreating(true);
    setError(null);
    try {
      const created = await createProject(appName.trim());
      setRevealedKey(created);
      setAppName("");
      await load();
    } catch (err) {
      setError(errorMessage(err, "Could not create project."));
    } finally {
      setCreating(false);
    }
  };

  const handleRegenerate = async (projectId: string) => {
    if (
      !window.confirm(
        "Regenerate the API key? The old key will stop working immediately.",
      )
    ) {
      return;
    }
    try {
      const updated = await regenerateApiKey(projectId);
      setRevealedKey(updated);
      await load();
    } catch (err) {
      setError(errorMessage(err, "Could not regenerate key."));
    }
  };

  const copyKey = async () => {
    if (!revealedKey) return;
    await navigator.clipboard.writeText(revealedKey.api_key);
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Projects</h1>
        <p className="text-sm text-slate-500">
          Create a project to get an API key for the SnapReport SDK.
        </p>
      </div>

      {error && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      {/* One-time API key reveal */}
      {revealedKey && (
        <div className="rounded-xl border border-amber-300 bg-amber-50 p-4">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="font-semibold text-amber-900">
                API key for “{revealedKey.app_name}”
              </h2>
              <p className="text-sm text-amber-700">
                Copy this now — for security it is shown only once and cannot be
                retrieved later.
              </p>
            </div>
            <button
              onClick={() => setRevealedKey(null)}
              className="text-amber-700 hover:text-amber-900"
              aria-label="Dismiss"
            >
              ✕
            </button>
          </div>
          <div className="mt-3 flex items-center gap-2">
            <code className="flex-1 overflow-x-auto rounded-lg bg-white px-3 py-2 text-sm text-slate-800">
              {revealedKey.api_key}
            </code>
            <button
              onClick={copyKey}
              className="rounded-lg bg-amber-600 px-3 py-2 text-sm font-medium text-white hover:bg-amber-700"
            >
              {copied ? "Copied!" : "Copy"}
            </button>
          </div>
        </div>
      )}

      {/* Create project form */}
      <form
        onSubmit={handleCreate}
        className="flex items-end gap-3 rounded-xl border border-slate-200 bg-white p-4"
      >
        <div className="flex-1">
          <label className="mb-1 block text-sm font-medium text-slate-700">
            App name
          </label>
          <input
            value={appName}
            onChange={(e) => setAppName(e.target.value)}
            placeholder="e.g. Shopping App"
            className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
          />
        </div>
        <button
          type="submit"
          disabled={creating}
          className="rounded-lg bg-brand-600 px-4 py-2 text-sm font-semibold text-white hover:bg-brand-700 disabled:opacity-60"
        >
          {creating ? "Creating…" : "Create project"}
        </button>
      </form>

      {/* Project list */}
      <div className="overflow-hidden rounded-xl border border-slate-200 bg-white">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>
              <th className="px-4 py-3">App name</th>
              <th className="px-4 py-3">API key prefix</th>
              <th className="px-4 py-3">Created</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  Loading…
                </td>
              </tr>
            ) : projects.length === 0 ? (
              <tr>
                <td colSpan={5} className="px-4 py-6 text-center text-slate-400">
                  No projects yet. Create one above.
                </td>
              </tr>
            ) : (
              projects.map((p) => (
                <tr key={p.id}>
                  <td className="px-4 py-3 font-medium text-slate-800">
                    {p.app_name}
                  </td>
                  <td className="px-4 py-3">
                    <code className="text-slate-600">{p.api_key_prefix}…</code>
                  </td>
                  <td className="px-4 py-3 text-slate-500">
                    {formatDate(p.created_at)}
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        p.is_active
                          ? "bg-green-100 text-green-700"
                          : "bg-slate-200 text-slate-500"
                      }`}
                    >
                      {p.is_active ? "Active" : "Disabled"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleRegenerate(p.id)}
                      className="text-sm font-medium text-brand-600 hover:underline"
                    >
                      Regenerate key
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
