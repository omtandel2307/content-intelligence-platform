"use client";

import { FormEvent, useEffect, useState } from "react";

export type Account = {
  id: string;
  displayName: string;
  email: string | null;
  createdAt: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
const accountCookieName = "cip_account_id";

export function accountHeaders(accountId?: string | null): Record<string, string> {
  return accountId ? { "X-Account-Id": accountId } : {};
}

export function readAccountIdCookie() {
  if (typeof document === "undefined") {
    return "";
  }

  return document.cookie
    .split("; ")
    .find((cookie) => cookie.startsWith(`${accountCookieName}=`))
    ?.split("=")[1] || "";
}

export function AccountSwitcher({
  initialAccountId,
}: {
  initialAccountId: string;
}) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedAccountId, setSelectedAccountId] = useState(initialAccountId);
  const [displayName, setDisplayName] = useState("");
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    async function loadAccounts() {
      try {
        const response = await fetch(`${apiBaseUrl}/api/accounts`);

        if (!response.ok) {
          throw new Error("Could not load accounts.");
        }

        setAccounts((await response.json()) as Account[]);
      } catch (exception) {
        setError(
          exception instanceof Error
            ? exception.message
            : "Could not load accounts."
        );
      } finally {
        setIsLoading(false);
      }
    }

    loadAccounts();
  }, []);

  function selectAccount(accountId: string) {
    document.cookie = `${accountCookieName}=${accountId}; path=/; max-age=31536000; samesite=lax`;
    setSelectedAccountId(accountId);
    window.location.reload();
  }

  function clearAccountSelection() {
    document.cookie = `${accountCookieName}=; path=/; max-age=0; samesite=lax`;
    setSelectedAccountId("");
    window.location.reload();
  }

  async function createAccount(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!displayName.trim()) {
      setError("Enter a name for the account.");
      return;
    }

    setError("");
    setIsCreating(true);

    try {
      const response = await fetch(`${apiBaseUrl}/api/accounts`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ displayName }),
      });
      const account = (await response.json()) as Account;

      if (!response.ok) {
        throw new Error("Could not create account.");
      }

      selectAccount(account.id);
    } catch (exception) {
      setError(
        exception instanceof Error
          ? exception.message
          : "Could not create account."
      );
    } finally {
      setIsCreating(false);
    }
  }

  const selectedAccount = accounts.find(
    (account) => account.id === selectedAccountId
  );
  const hasStaleSelection = Boolean(
    selectedAccountId && !isLoading && !error && !selectedAccount
  );

  return (
    <section className="account-card">
      <div className="stack">
        <p className="eyebrow">Account</p>
        <p className="status-text">
          {selectedAccount
            ? `Using ${selectedAccount.displayName}`
            : error && selectedAccountId
              ? "Could not verify the selected account yet."
            : hasStaleSelection
              ? "That saved account is no longer available. Choose another account or clear the selection."
              : selectedAccountId
              ? "Selected account is loading..."
              : "Create or choose an account to save your own library and chat history."}
        </p>
      </div>

      <div className="account-actions">
        <select
          className="select-input"
          disabled={isLoading || accounts.length === 0}
          value={selectedAccountId}
          onChange={(event) => selectAccount(event.target.value)}
        >
          <option value="">
            {isLoading ? "Loading accounts..." : "Choose account"}
          </option>
          {hasStaleSelection ? (
            <option value={selectedAccountId}>Missing account</option>
          ) : null}
          {accounts.map((account) => (
            <option key={account.id} value={account.id}>
              {account.displayName}
            </option>
          ))}
        </select>

        {hasStaleSelection ? (
          <button
            className="button button-muted"
            type="button"
            onClick={clearAccountSelection}
          >
            Clear selection
          </button>
        ) : null}

        <form className="account-create-form" onSubmit={createAccount}>
          <input
            className="text-input"
            value={displayName}
            onChange={(event) => setDisplayName(event.target.value)}
            placeholder="New account name"
          />
          <button
            className="button button-dark"
            type="submit"
            disabled={isCreating}
          >
            {isCreating ? "Creating..." : "Create"}
          </button>
        </form>
      </div>

      {error ? <p className="status-warning">{error}</p> : null}
    </section>
  );
}
