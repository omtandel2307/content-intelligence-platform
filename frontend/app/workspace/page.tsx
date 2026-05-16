import Link from "next/link";
import { cookies } from "next/headers";
import { AccountSwitcher } from "../../components/account-switcher";
import { MyWorkspacePanel } from "../../components/my-workspace-panel";

const accountCookieName = "cip_account_id";

export default async function WorkspacePage() {
  const cookieStore = await cookies();
  const accountId = cookieStore.get(accountCookieName)?.value || "";

  return (
    <main className="app-shell">
      <section className="page-wrap page-wrap-wide">
        <nav className="top-nav">
          <Link className="nav-brand" href="/">
            Content Intelligence
          </Link>
          <div className="nav-actions">
            <Link className="nav-link" href="/">
              Search
            </Link>
            <Link className="nav-link" href="/library">
              Library
            </Link>
          </div>
        </nav>

        <header className="workspace-page-header">
          <div className="stack">
            <p className="eyebrow">My Workspace</p>
            <h1 className="page-title">Your AI learning dashboard.</h1>
            <p className="body-copy">
              Saved videos, cross-library chat, knowledge maps, comparisons,
              project plans, and your learning timeline all live here.
            </p>
          </div>
          <AccountSwitcher initialAccountId={accountId} />
        </header>

        <MyWorkspacePanel accountId={accountId} />
      </section>
    </main>
  );
}
