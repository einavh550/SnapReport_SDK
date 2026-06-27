import { Link } from "react-router-dom";
import { resolveImageUrl } from "../api/client";
import type { TicketListItem } from "../types";
import StatusBadge from "./StatusBadge";

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleString();
}

export default function TicketCard({ ticket }: { ticket: TicketListItem }) {
  const img = resolveImageUrl(ticket.imageUrl);

  return (
    <Link
      to={`/tickets/${ticket.id}`}
      className="group flex flex-col overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm transition hover:shadow-md"
    >
      <div className="flex h-44 items-center justify-center bg-slate-100">
        {img ? (
          <img
            src={img}
            alt="Screenshot"
            className="h-full w-full object-contain"
          />
        ) : (
          <span className="text-sm text-slate-400">No screenshot</span>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex items-center justify-between">
          <StatusBadge status={ticket.status} />
          <span className="text-xs text-slate-400">
            {formatDate(ticket.timestamp)}
          </span>
        </div>
        <p className="line-clamp-2 text-sm font-medium text-slate-800">
          {ticket.description || "(No description)"}
        </p>
        <div className="mt-auto flex items-center justify-between text-xs text-slate-500">
          <span>{ticket.deviceSummary}</span>
          {ticket.appVersion && <span>v{ticket.appVersion}</span>}
        </div>
      </div>
    </Link>
  );
}
