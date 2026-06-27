import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { errorMessage } from "../api/client";
import { listReports } from "../api/reports";
import StatusBadge from "../components/StatusBadge";
import type { TicketListItem, TicketStatus } from "../types";

function StatCard({
  label,
  value,
  accent,
}: {
  label: string;
  value: number;
  accent: string;
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <p className="text-sm text-slate-500">{label}</p>
      <p className={`mt-1 text-3xl font-bold ${accent}`}>{value}</p>
    </div>
  );
}

function androidVersionOf(deviceSummary: string): string {
  const match = deviceSummary.match(/Android\s+(.+)$/);
  return match ? match[1] : "Unknown";
}

export default function Dashboard() {
  const [reports, setReports] = useState<TicketListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        setReports(await listReports());
      } catch (err) {
        setError(errorMessage(err, "Could not load reports."));
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const stats = useMemo(() => {
    const byStatus: Record<TicketStatus, number> = {
      OPEN: 0,
      IN_PROGRESS: 0,
      RESOLVED: 0,
      ARCHIVED: 0,
    };
    const byAndroid: Record<string, number> = {};
    const byAppVersion: Record<string, number> = {};

    for (const r of reports) {
      byStatus[r.status] = (byStatus[r.status] ?? 0) + 1;
      const av = androidVersionOf(r.deviceSummary);
      byAndroid[av] = (byAndroid[av] ?? 0) + 1;
      const appV = r.appVersion ?? "Unknown";
      byAppVersion[appV] = (byAppVersion[appV] ?? 0) + 1;
    }
    return { byStatus, byAndroid, byAppVersion };
  }, [reports]);

  const recent = reports.slice(0, 5);

  if (loading) {
    return <p className="text-slate-400">Loading dashboard…</p>;
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Dashboard</h1>
        <p className="text-sm text-slate-500">Overview of incoming bug reports.</p>
      </div>

      {error && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
        <StatCard label="Total reports" value={reports.length} accent="text-slate-900" />
        <StatCard label="Open" value={stats.byStatus.OPEN} accent="text-amber-600" />
        <StatCard
          label="In progress"
          value={stats.byStatus.IN_PROGRESS}
          accent="text-blue-600"
        />
        <StatCard
          label="Resolved"
          value={stats.byStatus.RESOLVED}
          accent="text-green-600"
        />
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        <Breakdown title="Reports by Android version" data={stats.byAndroid} />
        <Breakdown title="Reports by app version" data={stats.byAppVersion} />
      </div>

      <div className="rounded-xl border border-slate-200 bg-white">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-3">
          <h2 className="font-semibold text-slate-800">Recent reports</h2>
          <Link to="/tickets" className="text-sm font-medium text-brand-600 hover:underline">
            View all
          </Link>
        </div>
        {recent.length === 0 ? (
          <p className="px-5 py-6 text-center text-sm text-slate-400">
            No reports yet.
          </p>
        ) : (
          <ul className="divide-y divide-slate-100">
            {recent.map((r) => (
              <li key={r.id}>
                <Link
                  to={`/tickets/${r.id}`}
                  className="flex items-center justify-between px-5 py-3 hover:bg-slate-50"
                >
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-slate-800">
                      {r.description || "(No description)"}
                    </p>
                    <p className="text-xs text-slate-500">{r.deviceSummary}</p>
                  </div>
                  <StatusBadge status={r.status} />
                </Link>
              </li>
            ))}
          </ul>
        )}
      </div>
    </div>
  );
}

function Breakdown({
  title,
  data,
}: {
  title: string;
  data: Record<string, number>;
}) {
  const entries = Object.entries(data).sort((a, b) => b[1] - a[1]);
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5">
      <h2 className="mb-3 font-semibold text-slate-800">{title}</h2>
      {entries.length === 0 ? (
        <p className="text-sm text-slate-400">No data.</p>
      ) : (
        <ul className="space-y-2">
          {entries.map(([key, count]) => (
            <li key={key} className="flex items-center justify-between text-sm">
              <span className="text-slate-600">{key}</span>
              <span className="font-medium text-slate-900">{count}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
