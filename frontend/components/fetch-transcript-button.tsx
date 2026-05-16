"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { accountHeaders } from "./account-switcher";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function FetchTranscriptButton({
  videoId,
  accountId,
}: {
  videoId: string;
  accountId: string;
}) {
  const router = useRouter();
  const [status, setStatus] = useState<
    "idle" | "fetching" | "ready" | "unavailable" | "error"
  >("idle");

  async function fetchTranscript() {
    setStatus("fetching");

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/transcript`,
        {
          method: "POST",
          headers: accountHeaders(accountId),
        }
      );

      if (!response.ok) {
        throw new Error("Transcript request failed");
      }

      const transcript = await response.json();

      if (transcript.status === "READY") {
        setStatus("ready");
        router.refresh();
        return;
      }

      setStatus("unavailable");
      router.refresh();
    } catch {
      setStatus("error");
    }
  }

  return (
    <div className="stack">
      <button
        className={`button ${status === "ready" ? "button-success" : "button-primary"}`}
        type="button"
        onClick={fetchTranscript}
        disabled={status === "fetching"}
      >
        {status === "fetching"
          ? "Fetching transcript..."
          : status === "ready"
            ? "Transcript fetched"
            : "Fetch transcript"}
      </button>

      {status === "unavailable" ? (
        <p className="status-warning">
          Captions were not available for this video.
        </p>
      ) : null}

      {status === "error" ? (
        <p className="status-error">
          Could not fetch the transcript. Check the backend logs and try again.
        </p>
      ) : null}
    </div>
  );
}
