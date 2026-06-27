import { useEffect, useState } from "react";
import { errorMessage } from "../api/client";
import { listProjects } from "../api/projects";
import { listReports } from "../api/reports";
import TicketCard from "../components/TicketCard";
import TicketFiltersBar from "../components/TicketFilters";
import type { Project, TicketFilters, TicketListItem } from "../types";

export default function TicketFeed() {
  const [tickets, setTickets] = useState<TicketListItem[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [filters, setFilters] = useState<TicketFilters>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listProjects()
      .then(setProjects)
      .catch(() => setProjects([]));
  }, []);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    listReports(filters)
      .then((data) => {
        if (!cancelled) setTickets(data);
      })
      .catch((err) => {
        if (!cancelled) setError(errorMessage(err, "Could not load tickets."));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [filters]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Tickets</h1>
        <p className="text-sm text-slate-500">
          Visual bug reports from your apps.
        </p>
      </div>

      <TicketFiltersBar
        filters={filters}
        projects={projects}
        onChange={setFilters}
        onClear={() => setFilters({})}
      />

      {error && (
        <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-700">
          {error}
        </p>
      )}

      {loading ? (
        <p className="text-slate-400">Loading tickets…</p>
      ) : tickets.length === 0 ? (
        <div className="rounded-xl border border-dashed border-slate-300 bg-white py-16 text-center text-slate-400">
          No tickets match these filters.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {tickets.map((t) => (
            <TicketCard key={t.id} ticket={t} />
          ))}
        </div>
      )}
    </div>
  );
}
