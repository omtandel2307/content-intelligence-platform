"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function GenerateSummaryButton({
  videoId,
  disabled,
}: {
  videoId: string;
  disabled: boolean;
}) {
  const router = useRouter();
  const [status, setStatus] = useState<"idle" | "generating" | "ready" | "error">(
    "idle"
  );
  const [error, setError] = useState("");

  async function generateSummary() {
    setStatus("generating");
    setError("");

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/summary`,
        { method: "POST" }
      );
      const summary = await response.json();

      if (!response.ok || summary.status !== "READY") {
        throw new Error(summary.failureReason || "Summary generation failed.");
      }

      setStatus("ready");
      router.refresh();
    } catch (requestError) {
      setStatus("error");
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Could not generate summary."
      );
    }
  }

  return (
    <div className="stack">
      <button
        className={`button ${
          disabled
            ? "button-muted"
            : status === "ready"
              ? "button-success"
              : "button-primary"
        }`}
        type="button"
        onClick={generateSummary}
        disabled={disabled || status === "generating"}
      >
        {status === "generating"
          ? "Generating summary..."
          : status === "ready"
            ? "Summary generated"
            : "Generate summary"}
      </button>

      {disabled ? (
        <p className="status-text">
          Fetch a transcript before generating a summary.
        </p>
      ) : null}

      {status === "error" ? (
        <p className="status-error">{error}</p>
      ) : null}
    </div>
  );
}
