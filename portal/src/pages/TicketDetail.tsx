import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { errorMessage, resolveImageUrl } from "../api/client";
import { getReport, updateReportStatus } from "../api/reports";
import StatusBadge from "../components/StatusBadge";
import { TICKET_STATUSES, type TicketDetail, type TicketStatus } from "../types";

function prettifyKey(key: string): string {
  return key
    .replace(/_/g, " ")
    .replace(/([a-z])([A-Z])/g, "$1 $2")
    .replace(/^./, (c) => c.toUpperCase());
}

function formatValue(value: unknown): string {
  if (value === null || value === undefined) return "—";
  if (typeof value === "boolean") return value ? "Yes" : "No";
  return String(value);
}

function MetadataTable({ data }: { data: Record<string, unknown> }) {
  const entries = Object.entries(data);
  if (entries.length === 0) {
    return <p className="text-sm text-slate-400">No data.</p>;
  }
  return (
    <dl className="divide-y divide-slate-100">
      {entries.map(([key, value]) => (
        <div key={key} className="flex justify-between gap-4 py-1.5 text-sm">
          <dt className="text-slate-500">{prettifyKey(key)}</dt>
          <dd className="text-right font-medium text-slate-800">
            {formatValue(value)}
          </dd>
        </div>
      ))}
    </dl>
  );
}

export default function TicketDetailPage() {
  const { ticketId } = useParams<{ ticketId: string }>();
  const [ticket, setTicket] = useState<TicketDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!ticketId) return;
    setLoading(true);
    getReport(ticketId)
      .then(setTicket)
      .catch((err) => setError(errorMessage(err, "Could not load ticket.")))
      .finally(() => setLoading(false));
  }, [ticketId]);

  const handleStatusChange = async (status: TicketStatus) => {
    if (!ticketId) return;
    setSaving(true);
    setError(null);
    try {
      const updated = await updateReportStatus(ticketId, status);
      setTicket(updated);
    } catch (err) {
      setError(errorMessage(err, "Could not update status."));
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p className="text-slate-400">Loading ticket…</p>;
  if (error && !ticket) {
    return (
      <div className="space-y-4">
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
        <Link to="/tickets" className="text-sm text-brand-600 hover:underline">
          ← Back to tickets
        </Link>
      </div>
    );
  }
  if (!ticket) return null;

  const img = resolveImageUrl(ticket.imageUrl);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <Link to="/tickets" className="text-sm text-brand-600 hover:underline">
          ← Back to tickets
        </Link>
        <StatusBadge status={ticket.status} />
      </div>

      {error && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        {/* Left: screenshot */}
        <div className="rounded-xl border border-slate-200 bg-white p-4">
          <div className="flex min-h-[400px] items-center justify-center rounded-lg bg-slate-100">
            {img ? (
              <img
                src={img}
                alt="Screenshot"
                className="max-h-[600px] w-full object-contain"
              />
            ) : (
              <div className="text-center text-sm text-slate-400">
                <p>No screenshot</p>
                {ticket.screenshotBlockedReason && (
                  <p className="mt-1">
                    Blocked: {ticket.screenshotBlockedReason}
                  </p>
                )}
              </div>
            )}
          </div>
        </div>

        {/* Right: details */}
        <div className="space-y-5">
          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <label className="mb-1 block text-sm font-medium text-slate-700">
              Status
            </label>
            <select
              value={ticket.status}
              disabled={saving}
              onChange={(e) =>
                handleStatusChange(e.target.value as TicketStatus)
              }
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500"
            >
              {TICKET_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {s}
                </option>
              ))}
            </select>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="mb-2 font-semibold text-slate-800">Description</h2>
            <p className="text-sm text-slate-600">
              {ticket.description || "(No description provided)"}
            </p>
            <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
              <div>
                <p className="text-slate-500">User ID</p>
                <p className="font-medium text-slate-800">
                  {ticket.userId || "—"}
                </p>
              </div>
              <div>
                <p className="text-slate-500">Reported</p>
                <p className="font-medium text-slate-800">
                  {new Date(ticket.timestamp).toLocaleString()}
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="mb-2 font-semibold text-slate-800">Device</h2>
            <MetadataTable data={ticket.deviceMetadata} />
          </div>

          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <h2 className="mb-2 font-semibold text-slate-800">App</h2>
            <MetadataTable data={ticket.appMetadata} />
          </div>
        </div>
      </div>
    </div>
  );
}
