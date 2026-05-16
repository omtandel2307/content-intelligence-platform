"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

type TimelineEvent = {
  type: string;
  title: string;
  description: string;
  videoId: string | null;
  occurredAt: string;
};

type TimelineResponse = {
  accountId: string;
  events: TimelineEvent[];
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function LearningTimelinePanel({
  accountId,
  disabled,
}: {
  accountId: string;
  disabled: boolean;
}) {
  const [events, setEvents] = useState<TimelineEvent[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  async function loadTimeline() {
    if (disabled) {
      return;
    }

    setError("");
    setIsLoading(true);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/accounts/${encodeURIComponent(
          accountId
        )}/learning/timeline`
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(body?.message || body?.error || "Timeline failed.");
      }

      setEvents((body as TimelineResponse).events || []);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Timeline failed.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    loadTimeline();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accountId, disabled]);

  return (
    <div className="learning-card">
      <div className="workspace-header">
        <div className="stack">
          <p className="eyebrow">Learning Timeline</p>
          <h3 className="section-title">See how your workspace evolved.</h3>
          <p className="body-copy">
            A timeline of saves, transcripts, summaries, quizzes, and AI
            questions for this account.
          </p>
        </div>
        <button
          className={`button ${disabled ? "button-muted" : "button-dark"}`}
          type="button"
          onClick={loadTimeline}
          disabled={disabled || isLoading}
        >
          {isLoading ? "Loading..." : "Refresh"}
        </button>
      </div>

      {error ? <p className="status-warning">{error}</p> : null}

      {events.length > 0 ? (
        <div className="timeline-list">
          {events.map((event) => (
            <div
              className="timeline-item"
              key={`${event.type}-${event.videoId || "library"}-${event.occurredAt}`}
            >
              <span className="timeline-dot" />
              <div className="timeline-content">
                <div className="meta-row">
                  <span className="surface-chip">{event.type}</span>
                  <small>{new Date(event.occurredAt).toLocaleString()}</small>
                </div>
                {event.videoId ? (
                  <Link className="source-title-link" href={`/video/${event.videoId}`}>
                    {event.title}
                  </Link>
                ) : (
                  <strong>{event.title}</strong>
                )}
                <p className="video-description">{event.description}</p>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p className="status-text">
          Your timeline will appear after you save videos and use AI features.
        </p>
      )}
    </div>
  );
}
