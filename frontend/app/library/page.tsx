import Link from "next/link";
import { cookies } from "next/headers";
import { AccountSwitcher } from "../../components/account-switcher";

type SavedContentItem = {
  videoId: string;
  title: string;
  description: string;
  channelTitle: string;
  thumbnailUrl: string;
  publishedAt: string;
  duration: string;
  viewCount: string;
  likeCount: string;
  savedAt: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

const accountCookieName = "cip_account_id";

async function getSavedContent(accountId: string) {
  if (!accountId) {
    return [];
  }

  try {
    const response = await fetch(`${apiBaseUrl}/api/content`, {
      cache: "no-store",
      headers: {
        "X-Account-Id": accountId,
      },
    });

    if (!response.ok) {
      return [];
    }

    return (await response.json()) as SavedContentItem[];
  } catch {
    return [];
  }
}

function formatDate(value?: string) {
  if (!value) {
    return "Unknown";
  }

  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(value));
}

function cleanDescription(value?: string) {
  if (!value) {
    return "No description available.";
  }

  const stopMarkers = [
    "If you love watching",
    "Follow ",
    "Watch more:",
    "https://youtu.be/",
    "Subscribe",
    "Visit https://",
    "TED's videos may be used",
    "#",
  ];
  const markerIndexes = stopMarkers
    .map((marker) => value.indexOf(marker))
    .filter((index) => index >= 0);
  const trimmedDescription =
    markerIndexes.length > 0
      ? value.slice(0, Math.min(...markerIndexes))
      : value;
  const singleParagraph = trimmedDescription.trim().split(/\n\s*\n/)[0] || "";

  return singleParagraph.trim() || "No description available.";
}

export default async function LibraryPage() {
  const cookieStore = await cookies();
  const accountId = cookieStore.get(accountCookieName)?.value || "";
  const savedItems = await getSavedContent(accountId);

  return (
    <main className="app-shell">
      <section className="page-wrap">
        <nav className="top-nav">
          <Link className="nav-brand" href="/">
            Content Intelligence
          </Link>
          <div className="nav-actions">
            <Link className="nav-link" href="/">
              Search
            </Link>
            <Link className="nav-link" href="/workspace">
              Workspace
            </Link>
            <p className="nav-pill">{savedItems.length} saved videos</p>
          </div>
        </nav>

        <AccountSwitcher initialAccountId={accountId} />

        <header className="hero-card">
          <p className="eyebrow">Library</p>
          <h1 className="page-title">Your content command center.</h1>
          <p className="body-copy">
            Every saved video becomes a workspace for transcripts, OpenAI
            summaries, and RAG conversations.
          </p>
        </header>

        {!accountId ? (
          <section className="empty-state">
            <p className="eyebrow">Account required</p>
            <h2 className="section-title">Choose an account first.</h2>
            <p className="body-copy">
              Your library is account-specific now. Create or select an account
              above to see saved videos.
            </p>
          </section>
        ) : savedItems.length === 0 ? (
          <section className="empty-state">
            <p className="eyebrow">Empty for now</p>
            <h2 className="section-title">No saved videos yet.</h2>
            <p className="body-copy">
              Search for a video, open its details page, and save it to start
              building your library.
            </p>
          </section>
        ) : (
          <section className="library-grid">
            {savedItems.map((item) => (
              <Link
                key={item.videoId}
                className="video-card"
                href={`/video/${item.videoId}`}
              >
                {item.thumbnailUrl ? (
                  <img
                    className="video-thumb"
                    src={item.thumbnailUrl}
                    alt={item.title}
                  />
                ) : null}

                <div className="video-card-body">
                  <span className="surface-chip">{item.channelTitle}</span>
                  <h2 className="video-title">{item.title}</h2>
                  <p className="video-description video-description-clamped">
                    {cleanDescription(item.description)}
                  </p>
                  <div className="meta-row">
                    <span>Saved {formatDate(item.savedAt)}</span>
                    {/* <span>Open workspace</span> */}
                  </div>
                </div>
              </Link>
            ))}
          </section>
        )}
      </section>
    </main>
  );
}
