import { TICKET_STATUSES, type Project, type TicketFilters } from "../types";

interface Props {
  filters: TicketFilters;
  projects: Project[];
  onChange: (filters: TicketFilters) => void;
  onClear: () => void;
}

const inputClass =
  "w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-brand-500 focus:outline-none focus:ring-1 focus:ring-brand-500";
const labelClass = "block text-xs font-medium text-slate-500 mb-1";

export default function TicketFiltersBar({
  filters,
  projects,
  onChange,
  onClear,
}: Props) {
  const update = (patch: Partial<TicketFilters>) =>
    onChange({ ...filters, ...patch });

  return (
    <div className="grid grid-cols-2 gap-4 rounded-xl border border-slate-200 bg-white p-4 md:grid-cols-3 lg:grid-cols-4">
      <div>
        <label className={labelClass}>Project</label>
        <select
          className={inputClass}
          value={filters.projectId ?? ""}
          onChange={(e) => update({ projectId: e.target.value || undefined })}
        >
          <option value="">All projects</option>
          {projects.map((p) => (
            <option key={p.id} value={p.id}>
              {p.app_name}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className={labelClass}>Status</label>
        <select
          className={inputClass}
          value={filters.status ?? ""}
          onChange={(e) =>
            update({ status: (e.target.value as TicketFilters["status"]) || "" })
          }
        >
          <option value="">All statuses</option>
          {TICKET_STATUSES.map((s) => (
            <option key={s} value={s}>
              {s}
            </option>
          ))}
        </select>
      </div>

      <div>
        <label className={labelClass}>Android version</label>
        <input
          className={inputClass}
          placeholder="e.g. 15"
          value={filters.osVersion ?? ""}
          onChange={(e) => update({ osVersion: e.target.value || undefined })}
        />
      </div>

      <div>
        <label className={labelClass}>App version</label>
        <input
          className={inputClass}
          placeholder="e.g. 2.4.1"
          value={filters.appVersion ?? ""}
          onChange={(e) => update({ appVersion: e.target.value || undefined })}
        />
      </div>

      <div>
        <label className={labelClass}>Device model</label>
        <input
          className={inputClass}
          placeholder="e.g. Pixel 7"
          value={filters.deviceModel ?? ""}
          onChange={(e) => update({ deviceModel: e.target.value || undefined })}
        />
      </div>

      <div>
        <label className={labelClass}>From date</label>
        <input
          type="date"
          className={inputClass}
          value={filters.dateFrom ?? ""}
          onChange={(e) => update({ dateFrom: e.target.value || undefined })}
        />
      </div>

      <div>
        <label className={labelClass}>To date</label>
        <input
          type="date"
          className={inputClass}
          value={filters.dateTo ?? ""}
          onChange={(e) => update({ dateTo: e.target.value || undefined })}
        />
      </div>

      <div className="flex items-end">
        <button
          onClick={onClear}
          className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50"
        >
          Clear filters
        </button>
      </div>
    </div>
  );
}
