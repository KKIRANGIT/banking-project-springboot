import { useState } from "react";

const defaultRegister = {
  username: "",
  password: "",
  role: "TELLER",
};

const defaultLogin = {
  username: "",
  password: "",
};

export default function AuthPanel({ session, busy, onRegister, onLogin, onLogout }) {
  const [registerForm, setRegisterForm] = useState(defaultRegister);
  const [loginForm, setLoginForm] = useState(defaultLogin);

  return (
    <section className="panel p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-slate-500">Identity</p>
          <h2 className="mt-2 font-display text-2xl text-ink">Authentication desk</h2>
          <p className="mt-2 max-w-xl text-sm leading-7 text-slate-600">
            Register a teller or admin, sign in, and keep the JWT in local state for account and payment actions.
          </p>
        </div>
        {session ? (
          <button className="button-secondary" type="button" onClick={onLogout}>
            Log out
          </button>
        ) : null}
      </div>

      <div className="mt-6 grid gap-5 xl:grid-cols-2">
        <form
          className="rounded-[1.75rem] border border-slate-200 bg-slate-50/70 p-5"
          onSubmit={(event) => {
            event.preventDefault();
            onRegister(registerForm).then(() => setRegisterForm(defaultRegister));
          }}
        >
          <h3 className="font-semibold text-slate-900">Create operator</h3>
          <div className="mt-4 grid gap-3">
            <input
              className="field"
              placeholder="Username"
              value={registerForm.username}
              onChange={(event) => setRegisterForm((current) => ({ ...current, username: event.target.value }))}
            />
            <input
              className="field"
              type="password"
              placeholder="Password"
              value={registerForm.password}
              onChange={(event) => setRegisterForm((current) => ({ ...current, password: event.target.value }))}
            />
            <select
              className="field"
              value={registerForm.role}
              onChange={(event) => setRegisterForm((current) => ({ ...current, role: event.target.value }))}
            >
              <option value="TELLER">TELLER</option>
              <option value="ADMIN">ADMIN</option>
              <option value="CUSTOMER">CUSTOMER</option>
            </select>
            <button className="button-primary mt-1" disabled={busy} type="submit">
              Register user
            </button>
          </div>
        </form>

        <form
          className="rounded-[1.75rem] border border-slate-200 bg-white p-5"
          onSubmit={(event) => {
            event.preventDefault();
            onLogin(loginForm);
          }}
        >
          <h3 className="font-semibold text-slate-900">Open session</h3>
          <div className="mt-4 grid gap-3">
            <input
              className="field"
              placeholder="Username"
              value={loginForm.username}
              onChange={(event) => setLoginForm((current) => ({ ...current, username: event.target.value }))}
            />
            <input
              className="field"
              type="password"
              placeholder="Password"
              value={loginForm.password}
              onChange={(event) => setLoginForm((current) => ({ ...current, password: event.target.value }))}
            />
            <button className="button-primary mt-1" disabled={busy} type="submit">
              {session ? "Refresh session" : "Sign in"}
            </button>
          </div>

          <div className="mt-5 rounded-3xl bg-ink px-4 py-4 text-sm text-white">
            <div className="font-medium">{session ? `Signed in as ${session.username}` : "No active session"}</div>
            <div className="mt-1 text-white/70">
              {session ? `Role: ${session.role}` : "Transactions and account actions unlock after login."}
            </div>
          </div>
        </form>
      </div>
    </section>
  );
}
