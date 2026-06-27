import { API_BASE_URL } from "../api/client";
import { useAuth } from "../context/AuthContext";

export default function Settings() {
  const { email } = useAuth();

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Settings</h1>
        <p className="text-sm text-slate-500">Account and environment details.</p>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold text-slate-800">Account</h2>
        <div className="text-sm">
          <p className="text-slate-500">Signed in as</p>
          <p className="font-medium text-slate-800">{email ?? "Unknown"}</p>
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold text-slate-800">Backend</h2>
        <div className="text-sm">
          <p className="text-slate-500">API base URL</p>
          <code className="font-medium text-slate-800">{API_BASE_URL}</code>
        </div>
      </div>

      <div className="rounded-xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-800">
        <p className="font-medium">Security note</p>
        <p className="mt-1">
          For this demo, the JWT is stored in <code>localStorage</code>. In
          production, prefer secure, http-only cookie-based authentication to
          mitigate XSS token theft.
        </p>
      </div>
    </div>
  );
}
