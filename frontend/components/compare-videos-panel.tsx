"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { accountHeaders } from "./account-switcher";

type SavedVideo = {
  videoId: string;
  title: string;
  channelTitle: string;
  thumbnailUrl: string | null;
  transcriptStatus: string;
  transcriptReady: boolean;
};

type VideoBrief = {
  videoId: string;
  title: string;
  channelTitle: string;
};

type CompareResponse = {
  provider: string;
  model: string;
  firstVideo: VideoBrief;
  secondVideo: VideoBrief;
  commonGround: string[];
  firstVideoStrengths: string[];
  secondVideoStrengths: string[];
  disagreementsOrGaps: string[];
  bestForBeginners: string;
  bestForDepth: string;
  learningRecommendation: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function CompareVideosPanel({
  accountId,
  disabled,
}: {
  accountId: string;
  disabled: boolean;
}) {
  const [savedVideos, setSavedVideos] = useState<SavedVideo[]>([]);
  const [firstVideoId, setFirstVideoId] = useState("");
  const [secondVideoId, setSecondVideoId] = useState("");
  const [comparison, setComparison] = useState<CompareResponse | null>(null);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [isLoadingVideos, setIsLoadingVideos] = useState(false);
  const [isComparing, setIsComparing] = useState(false);

  useEffect(() => {
    if (!accountId) {
      return;
    }

    async function loadSavedVideos() {
      setIsLoadingVideos(true);
      try {
        const response = await fetch(`${apiBaseUrl}/api/content`, {
          headers: accountHeaders(accountId),
        });

        if (!response.ok) {
          throw new Error("Could not load saved videos.");
        }

        const videos = (await response.json()) as SavedVideo[];
        setSavedVideos(videos);
        setFirstVideoId(videos[0]?.videoId || "");
        setSecondVideoId(videos[1]?.videoId || "");
      } catch (exception) {
        setError(
          exception instanceof Error
            ? exception.message
            : "Could not load saved videos."
        );
      } finally {
        setIsLoadingVideos(false);
      }
    }

    loadSavedVideos();
  }, [accountId]);

  async function compareVideos() {
    if (!firstVideoId || !secondVideoId || firstVideoId === secondVideoId) {
      setError("Choose two different saved videos.");
      return;
    }

    setError("");
    setStatus("Comparing both transcripts with OpenAI...");
    setIsComparing(true);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/accounts/${encodeURIComponent(
          accountId
        )}/learning/compare`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ videoIds: [firstVideoId, secondVideoId] }),
        }
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(errorMessage(body, "Comparison failed."));
      }

      setComparison(body as CompareResponse);
      setStatus(`Compared with ${body.provider}: ${body.model}.`);
    } catch (exception) {
      setComparison(null);
      setError(exception instanceof Error ? exception.message : "Comparison failed.");
      setStatus("");
    } finally {
      setIsComparing(false);
    }
  }

  const isDisabled =
    disabled || isLoadingVideos || isComparing || savedVideos.length < 2;
  const selectedVideos = savedVideos.filter((video) =>
    [firstVideoId, secondVideoId].includes(video.videoId)
  );
  const selectedVideosNeedTranscripts = selectedVideos.some(
    (video) => !video.transcriptReady
  );
  const canCompare =
    !isDisabled &&
    Boolean(firstVideoId) &&
    Boolean(secondVideoId) &&
    firstVideoId !== secondVideoId &&
    !selectedVideosNeedTranscripts;

  return (
    <div className="learning-card">
      <div className="stack">
        <p className="eyebrow">Compare Videos</p>
        <h3 className="section-title">Find overlap, gaps, and best watch order.</h3>
        <p className="body-copy">
          Pick two saved videos with transcripts and let AI compare what
          each one teaches better.
        </p>
      </div>

      <div className="compare-controls">
        <select
          className="select-input"
          value={firstVideoId}
          onChange={(event) => setFirstVideoId(event.target.value)}
          disabled={isDisabled}
        >
          {savedVideos.map((video) => (
            <option key={video.videoId} value={video.videoId}>
              {video.transcriptReady ? "Ready" : "Needs transcript"} - {video.title}
            </option>
          ))}
        </select>

        <select
          className="select-input"
          value={secondVideoId}
          onChange={(event) => setSecondVideoId(event.target.value)}
          disabled={isDisabled}
        >
          {savedVideos.map((video) => (
            <option key={video.videoId} value={video.videoId}>
              {video.transcriptReady ? "Ready" : "Needs transcript"} - {video.title}
            </option>
          ))}
        </select>

        <button
          className={`button ${isDisabled ? "button-muted" : "button-dark"}`}
          type="button"
          onClick={compareVideos}
          disabled={!canCompare}
        >
          {isComparing ? "Comparing..." : "Compare"}
        </button>
      </div>

      {savedVideos.length < 2 ? (
        <p className="status-text">Save at least two videos to compare them.</p>
      ) : null}
      {savedVideos.length >= 2 && selectedVideosNeedTranscripts ? (
        <p className="status-warning">
          Compare only works for videos marked Ready. Open each selected video
          and click Fetch transcript first.
        </p>
      ) : null}
      {status ? <p className="status-text">{status}</p> : null}
      {error ? <p className="status-warning">{error}</p> : null}

      {comparison ? (
        <div className="learning-result-grid">
          <ResultList title="Common ground" items={comparison.commonGround} />
          <ResultList
            title={`${comparison.firstVideo.title} teaches better`}
            items={comparison.firstVideoStrengths}
          />
          <ResultList
            title={`${comparison.secondVideo.title} teaches better`}
            items={comparison.secondVideoStrengths}
          />
          <ResultList title="Gaps or differences" items={comparison.disagreementsOrGaps} />

          <div className="learning-wide-result">
            <p className="detail-label">Recommendation</p>
            <p className="body-copy">{comparison.learningRecommendation}</p>
            <p className="status-text">Beginner pick: {comparison.bestForBeginners}</p>
            <p className="status-text">Depth pick: {comparison.bestForDepth}</p>
            <div className="topic-video-links">
              <Link className="surface-chip" href={`/video/${comparison.firstVideo.videoId}`}>
                Open first video
              </Link>
              <Link className="surface-chip" href={`/video/${comparison.secondVideo.videoId}`}>
                Open second video
              </Link>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function errorMessage(body: unknown, fallback: string) {
  if (!body || typeof body !== "object") {
    return fallback;
  }

  const payload = body as { message?: string; error?: string; detail?: string };
  return payload.message || payload.detail || payload.error || fallback;
}

function ResultList({ title, items }: { title: string; items: string[] }) {
  return (
    <div className="learning-result-card">
      <p className="detail-label">{title}</p>
      {items.length > 0 ? (
        <ul className="formatted-list">
          {items.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      ) : (
        <p className="status-text">No strong evidence found.</p>
      )}
    </div>
  );
}
