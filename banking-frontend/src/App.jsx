import { useEffect, useMemo, useState } from "react";
import AccountPanel from "./components/AccountPanel";
import AuthPanel from "./components/AuthPanel";
import HealthPanel from "./components/HealthPanel";
import HeroPanel from "./components/HeroPanel";
import OverviewPanel from "./components/OverviewPanel";
import StatusBanner from "./components/StatusBanner";
import TransactionPanel from "./components/TransactionPanel";
import TransactionsPanel from "./components/TransactionsPanel";
import { api } from "./lib/api";

const SESSION_KEY = "banking-frontend-session";

function loadStoredSession() {
  const raw = window.localStorage.getItem(SESSION_KEY);
  return raw ? JSON.parse(raw) : null;
}

export default function App() {
  const [session, setSession] = useState(loadStoredSession);
  const [health, setHealth] = useState(null);
  const [activeAccountNumber, setActiveAccountNumber] = useState("");
  const [account, setAccount] = useState(null);
  const [transactions, setTransactions] = useState([]);
  const [busy, setBusy] = useState(false);
  const [toast, setToast] = useState(null);
  const [lastRefreshAt, setLastRefreshAt] = useState("just now");

  useEffect(() => {
    if (session) {
      window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
    } else {
      window.localStorage.removeItem(SESSION_KEY);
    }
  }, [session]);

  useEffect(() => {
    let mounted = true;

    async function refreshHealth() {
      try {
        const gatewayHealth = await api.getGatewayHealth();
        if (mounted) {
          setHealth(gatewayHealth);
          setLastRefreshAt(new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }));
        }
      } catch {
        if (mounted) {
          setHealth({ status: "UNAVAILABLE", components: {} });
        }
      }
    }

    refreshHealth();
    const intervalId = window.setInterval(refreshHealth, 15000);

    return () => {
      mounted = false;
      window.clearInterval(intervalId);
    };
  }, []);

  function pushToast(type, title, message) {
    setToast({ type, title, message });
  }

  async function withBusy(action) {
    setBusy(true);
    try {
      await action();
    } finally {
      setBusy(false);
    }
  }

  async function loadAccount(accountNumber) {
    if (!session?.token) {
      pushToast("error", "Authentication required", "Sign in before loading an account.");
      return;
    }

    await withBusy(async () => {
      const [accountResponse, transactionsResponse] = await Promise.all([
        api.getAccount(accountNumber, session.token),
        api.getTransactionsByAccount(accountNumber, session.token).catch(() => []),
      ]);

      setActiveAccountNumber(accountResponse.accountNumber);
      setAccount(accountResponse);
      setTransactions(Array.isArray(transactionsResponse) ? transactionsResponse : []);
      pushToast("success", "Account loaded", `Loaded ${accountResponse.accountHolderName}.`);
    });
  }

  async function handleRegister(payload) {
    await withBusy(async () => {
      const response = await api.registerUser(payload);
      pushToast("success", "User registered", `Created ${response.username} with role ${response.role}.`);
    });
  }

  async function handleLogin(payload) {
    await withBusy(async () => {
      const response = await api.login(payload);
      setSession(response);
      pushToast("success", "Session started", `Signed in as ${response.username}.`);
    });
  }

  async function handleCreateAccount(payload) {
    if (!session?.token) {
      pushToast("error", "Authentication required", "Sign in before creating an account.");
      return;
    }

    await withBusy(async () => {
      const response = await api.createAccount(payload, session.token);
      setAccount(response);
      setActiveAccountNumber(response.accountNumber);
      setTransactions([]);
      pushToast("success", "Account created", `Created account ${response.accountNumber}.`);
    });
  }

  async function handleCreateTransaction(payload) {
    if (!session?.token) {
      pushToast("error", "Authentication required", "Sign in before posting a transaction.");
      return;
    }

    await withBusy(async () => {
      const response = await api.createTransaction(payload, session.token);
      pushToast("success", "Transaction accepted", `Transaction #${response.id} marked ${response.status}.`);
      await loadAccount(payload.accountNumber);
    });
  }

  function handleError(error) {
    pushToast("error", "Request failed", error.message);
  }

  const safeHandlers = useMemo(
    () => ({
      register: (payload) => handleRegister(payload).catch(handleError),
      login: (payload) => handleLogin(payload).catch(handleError),
      lookup: (accountNumber) => loadAccount(accountNumber).catch(handleError),
      createAccount: (payload) => handleCreateAccount(payload).catch(handleError),
      createTransaction: (payload) => handleCreateTransaction(payload).catch(handleError),
    }),
    [session],
  );

  return (
    <div className="min-h-screen px-4 py-5 md:px-6 lg:px-8">
      <div className="mx-auto grid max-w-[1600px] gap-5 xl:grid-cols-[420px_minmax(0,1fr)]">
        <HeroPanel health={health} lastRefresh={lastRefreshAt} session={session} />

        <main className="space-y-5">
          <StatusBanner toast={toast} />

          <OverviewPanel
            account={account}
            health={health}
            session={session}
            transactions={transactions}
          />

          <AuthPanel
            busy={busy}
            onLogin={safeHandlers.login}
            onLogout={() => {
              setSession(null);
              setAccount(null);
              setTransactions([]);
              setActiveAccountNumber("");
              pushToast("info", "Session cleared", "You have been logged out on the frontend.");
            }}
            onRegister={safeHandlers.register}
            session={session}
          />

          <AccountPanel
            account={account}
            activeAccountNumber={activeAccountNumber}
            onCreate={safeHandlers.createAccount}
            onLookup={safeHandlers.lookup}
            token={session?.token}
          />

          <div className="grid gap-5 2xl:grid-cols-[1.1fr_0.9fr]">
            <TransactionPanel
              account={account}
              health={health}
              onSubmit={safeHandlers.createTransaction}
              token={session?.token}
            />
            <HealthPanel health={health} />
          </div>

          <TransactionsPanel
            activeAccountNumber={activeAccountNumber}
            onRefresh={safeHandlers.lookup}
            token={session?.token}
            transactions={transactions}
          />
        </main>
      </div>
    </div>
  );
}
