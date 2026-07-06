"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { AccountSwitcher } from "./account-switcher";

type SearchResult = {
  videoId: string;
  title: string;
  description: string;
  channelTitle: string;
  thumbnailUrl: string;
  publishedAt: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function YoutubeSearchShell({ accountId }: { accountId: string }) {
  const [query, setQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [results, setResults] = useState<SearchResult[]>([]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const trimmedQuery = query.trim();

    if (!trimmedQuery) {
      setError("Enter a search term to find videos.");
      setResults([]);
      return;
    }

    setLoading(true);
    setError("");

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/youtube/search?q=${encodeURIComponent(trimmedQuery)}`
      );

      if (!response.ok) {
        throw new Error("Search request failed.");
      }

      const payload = await response.json();
      setResults(payload.items ?? []);
    } catch (requestError) {
      setResults([]);
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Unable to search YouTube right now."
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="app-shell">
      <section className="page-wrap">
        <nav className="top-nav">
          <Link className="nav-brand" href="/">
            Content Intelligence
          </Link>
          <div className="nav-actions">
            <Link className="nav-link" href="/workspace">
              Workspace
            </Link>
            <Link className="nav-link" href="/library">
              Library
            </Link>
          </div>
        </nav>

        <section className="home-hero-layout">
          <div className="hero-card hero-card-search">
            <p className="eyebrow">Search first</p>
            <h1 className="hero-title">Find videos. Turn them into learning assets.</h1>
            <p className="body-copy">
              Search YouTube, save the useful ones, fetch transcripts, summarize
              with OpenAI, and use AI learning tools powered by your OpenAI key.
            </p>

            <form className="search-form search-form-large" onSubmit={onSubmit}>
              <input
                className="text-input"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Try: Spring Boot RAG, AI agents, system design"
              />
              <button
                className="button button-accent"
                type="submit"
                disabled={loading}
              >
                {loading ? "Searching..." : "Search"}
              </button>
            </form>

            {error ? <p className="status-error">{error}</p> : null}
          </div>

          <aside className="home-command-card">
            <div className="stack">
              <p className="eyebrow">Dashboard</p>
              <h2 className="section-title">Jump back in.</h2>
              <p className="body-copy">
                Your saved videos, maps, project plans, comparisons, and chats
                live in Workspace.
              </p>
            </div>

            <div className="home-action-grid">
              <Link className="button button-dark" href="/workspace">
                Open workspace
              </Link>
              <Link className="button button-muted" href="/library">
                View library
              </Link>
            </div>

            <AccountSwitcher initialAccountId={accountId} />
          </aside>
        </section>

        {results.length > 0 ? (
          <section className="card-grid" aria-label="Search results">
            {results.map((result) => (
              <Link
                key={result.videoId}
                className="video-card"
                href={{
                  pathname: `/video/${result.videoId}`,
                  query: {
                    title: result.title,
                    thumbnail: result.thumbnailUrl,
                    channel: result.channelTitle,
                  },
                }}
                aria-label={`Open ${result.title}`}
              >
                <img
                  className="video-thumb"
                  src={result.thumbnailUrl}
                  alt={result.title}
                />
                <div className="video-card-body">
                  <span className="surface-chip">{result.channelTitle}</span>
                  <h2 className="video-title">{result.title}</h2>
                  <p className="video-description">
                    {result.description || "No description provided by YouTube."}
                  </p>
                  <div className="meta-row">
                    <span>{new Date(result.publishedAt).toLocaleDateString()}</span>
                    <span>Open video</span>
                  </div>
                </div>
              </Link>
            ))}
          </section>
        ) : (
          <section className="home-feature-strip">
            <div>
              <p className="eyebrow">Step 1</p>
              <h2 className="section-title">Search</h2>
              <p className="status-text">Find a useful YouTube video.</p>
            </div>
            <div>
              <p className="eyebrow">Step 2</p>
              <h2 className="section-title">Save</h2>
              <p className="status-text">Add it to your account library.</p>
            </div>
            <div>
              <p className="eyebrow">Step 3</p>
              <h2 className="section-title">Learn</h2>
              <p className="status-text">Summarize, quiz, chat, map, compare.</p>
            </div>
          </section>
        )}
      </section>
    </main>
  );
}
