"use client";

import { useState } from "react";
import { accountHeaders } from "./account-switcher";

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function SaveVideoButton({
  videoId,
  accountId,
}: {
  videoId: string;
  accountId: string;
}) {
  const [status, setStatus] = useState<"idle" | "saving" | "saved" | "error">(
    "idle"
  );

  async function saveVideo() {
    setStatus("saving");

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/content/youtube/${encodeURIComponent(videoId)}`,
        {
          method: "POST",
          headers: accountHeaders(accountId),
        }
      );

      if (!response.ok) {
        throw new Error("Save failed");
      }

      setStatus("saved");
    } catch {
      setStatus("error");
    }
  }

  return (
    <div className="stack">
      <button
        className={`button ${status === "saved" ? "button-success" : "button-primary"}`}
        type="button"
        onClick={saveVideo}
        disabled={status === "saving" || status === "saved"}
      >
        {status === "saving" ? "Saving..." : status === "saved" ? "Saved" : "Save video"}
      </button>

      {status === "error" ? (
        <p className="status-error">
          Could not save this video. Select an account and check that the backend is running.
        </p>
      ) : null}
    </div>
  );
}
