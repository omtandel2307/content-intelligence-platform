import Link from "next/link";
import { cookies } from "next/headers";
import { AccountSwitcher } from "../../../components/account-switcher";
import { FetchTranscriptButton } from "../../../components/fetch-transcript-button";
import { FormattedAiText } from "../../../components/formatted-ai-text";
import { GenerateSummaryButton } from "../../../components/generate-summary-button";
import {
  ChatMessage,
  LocalRagChatPanel,
} from "../../../components/local-rag-chat-panel";
import { SaveVideoButton } from "../../../components/save-video-button";
import { TranscriptTogglePanel } from "../../../components/transcript-toggle-panel";
import {
  QuizDetails,
  VideoQuizPanel,
} from "../../../components/video-quiz-panel";

type VideoPageProps = {
  params: Promise<{
    videoId: string;
  }>;
};

type VideoDetails = {
  videoId: string;
  title: string;
  description: string;
  channelTitle: string;
  thumbnailUrl: string;
  publishedAt: string;
  duration: string;
  viewCount: string;
  likeCount: string;
};

type TranscriptDetails = {
  videoId: string;
  transcriptText: string | null;
  languageCode: string | null;
  status: "READY" | "UNAVAILABLE" | "FAILED";
  failureReason: string | null;
  fetchedAt: string;
};

type SummaryDetails = {
  videoId: string;
  summaryText: string | null;
  model: string | null;
  status: "READY" | "FAILED";
  failureReason: string | null;
  generatedAt: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
const accountCookieName = "cip_account_id";

async function getVideoDetails(videoId: string) {
  try {
    const response = await fetch(
      `${apiBaseUrl}/api/youtube/videos/${encodeURIComponent(videoId)}`,
      { cache: "no-store" }
    );

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as VideoDetails;
  } catch {
    return null;
  }
}

async function getTranscript(videoId: string) {
  try {
    const response = await fetch(
      `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/transcript`,
      { cache: "no-store" }
    );

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as TranscriptDetails;
  } catch {
    return null;
  }
}

async function getSummary(videoId: string) {
  try {
    const response = await fetch(
      `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/summary`,
      { cache: "no-store" }
    );

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as SummaryDetails;
  } catch {
    return null;
  }
}

async function getChatHistory(accountId: string, videoId: string) {
  if (!accountId) {
    return [];
  }

  try {
    const response = await fetch(
      `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/rag/chat`,
      {
        cache: "no-store",
        headers: {
          "X-Account-Id": accountId,
        },
      }
    );

    if (!response.ok) {
      return [];
    }

    return (await response.json()) as ChatMessage[];
  } catch {
    return [];
  }
}

async function getQuiz(videoId: string) {
  try {
    const response = await fetch(
      `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/quiz`,
      { cache: "no-store" }
    );

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as QuizDetails;
  } catch {
    return null;
  }
}

function formatNumber(value?: string) {
  const numberValue = Number(value);

  if (!Number.isFinite(numberValue) || numberValue <= 0) {
    return "Unavailable";
  }

  return new Intl.NumberFormat("en", {
    notation: "compact",
    maximumFractionDigits: 1,
  }).format(numberValue);
}

function formatDate(value?: string) {
  if (!value) {
    return "Unavailable";
  }

  return new Intl.DateTimeFormat("en", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(new Date(value));
}

function formatDuration(value?: string) {
  if (!value) {
    return "Unavailable";
  }

  const match = value.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);

  if (!match) {
    return value;
  }

  const [, hours = "0", minutes = "0", seconds = "0"] = match;
  const parts = [hours, minutes, seconds].map((part) =>
    Number(part).toString().padStart(2, "0")
  );

  return Number(hours) > 0 ? parts.join(":") : parts.slice(1).join(":");
}

function cleanDescription(value?: string) {
  if (!value) {
    return "No description available.";
  }

  const stopMarkers = [
    "If you love watching TED Talks",
    "Follow TED!",
    "Watch more:",
    "https://youtu.be/",
    "TED's videos may be used",
    "#TED",
  ];
  const markerIndexes = stopMarkers
    .map((marker) => value.indexOf(marker))
    .filter((index) => index >= 0);
  const trimmedDescription =
    markerIndexes.length > 0
      ? value.slice(0, Math.min(...markerIndexes))
      : value;

  return trimmedDescription.trim() || "No description available.";
}

export default async function VideoPage({ params }: VideoPageProps) {
  const { videoId } = await params;
  const cookieStore = await cookies();
  const accountId = cookieStore.get(accountCookieName)?.value || "";
  const video = await getVideoDetails(videoId);
  const transcript = await getTranscript(videoId);
  const summary = await getSummary(videoId);
  const chatHistory = await getChatHistory(accountId, videoId);
  const quiz = await getQuiz(videoId);
  const hasReadyTranscript =
    transcript?.status === "READY" && Boolean(transcript.transcriptText);

  return (
    <main className="app-shell">
      <section className="page-wrap">
        <nav className="top-nav">
          <Link className="nav-link" href="/">
            Back to search
          </Link>
          <Link className="nav-link" href="/library">
            Library
          </Link>
        </nav>

        <AccountSwitcher initialAccountId={accountId} />

        <div className="content-layout">
          <section className="stack">
            <div className="page-header">
              <p className="eyebrow">
                Video Details
              </p>
              <h1 className="page-title">
                {video?.title || "Selected video"}
              </h1>
              <p className="body-copy">
                {video?.channelTitle || "Channel unavailable"}
              </p>
            </div>

            {video?.thumbnailUrl ? (
              <img
                className="video-thumb"
                src={video.thumbnailUrl}
                alt={video.title}
                style={{ borderRadius: 24, border: "1px solid var(--border)" }}
              />
            ) : (
              <div className="empty-state" style={{ aspectRatio: "16 / 9" }}>
                Video preview unavailable
              </div>
            )}

            <div className="metric-grid">
              <Metric label="Duration" value={formatDuration(video?.duration)} />
              <Metric label="Views" value={formatNumber(video?.viewCount)} />
              <Metric label="Likes" value={formatNumber(video?.likeCount)} />
              <Metric label="Published" value={formatDate(video?.publishedAt)} />
            </div>
          </section>

          <aside className="panel side-panel">
            <div className="stack">
              <p className="detail-label">
                Video ID
              </p>
              <p className="detail-text" style={{ fontWeight: 900, wordBreak: "break-word" }}>
                {videoId}
              </p>
            </div>

            <div className="stack">
              <p className="detail-label">
                Description
              </p>
              <p className="detail-text">
                {cleanDescription(video?.description)}
              </p>
            </div>

            <SaveVideoButton videoId={videoId} accountId={accountId} />
            <FetchTranscriptButton videoId={videoId} accountId={accountId} />
            <GenerateSummaryButton
              videoId={videoId}
              disabled={!hasReadyTranscript}
            />
          </aside>
        </div>

        <section className="panel">
          <div className="stack">
            <p className="eyebrow">
              Summary
            </p>
            <h2 className="section-title">
              {summary?.status === "READY"
                ? "Generated summary"
                : summary?.status === "FAILED"
                  ? "Summary failed"
                  : "No summary yet"}
            </h2>
          </div>

          {summary?.status === "READY" && summary.summaryText ? (
            <FormattedAiText text={summary.summaryText} />
          ) : (
            <p className="body-copy">
              {summary?.failureReason ||
                "Generate a summary after the transcript is available."}
            </p>
          )}
        </section>

        <VideoQuizPanel
          videoId={videoId}
          disabled={!hasReadyTranscript}
          initialQuiz={quiz}
        />

        <LocalRagChatPanel
          videoId={videoId}
          accountId={accountId}
          disabled={!hasReadyTranscript}
          initialMessages={chatHistory}
        />

        <section className="panel">
          <div className="stack">
            <p className="eyebrow">
              Transcript
            </p>
            <h2 className="section-title">
              {transcript?.status === "READY"
                ? "Stored transcript"
                : transcript?.status === "UNAVAILABLE"
                  ? "Transcript unavailable"
                  : transcript?.status === "FAILED"
                    ? "Transcript failed"
                    : "No transcript yet"}
            </h2>
          </div>

          {transcript?.status === "READY" && transcript.transcriptText ? (
            <TranscriptTogglePanel transcriptText={transcript.transcriptText} />
          ) : (
            <p className="body-copy">
              {transcript?.failureReason ||
                "Fetch the transcript to prepare this video for summaries and chat."}
            </p>
          )}
        </section>
      </section>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="metric-card">
      <p className="metric-label">{label}</p>
      <p className="metric-value">{value}</p>
    </div>
  );
}
