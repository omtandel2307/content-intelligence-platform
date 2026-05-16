"use client";

import { useState } from "react";

export function TranscriptTogglePanel({
  transcriptText,
}: {
  transcriptText: string;
}) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div className="stack">
      <button
        className="button button-dark"
        type="button"
        onClick={() => setIsOpen((current) => !current)}
      >
        {isOpen ? "Hide stored transcript" : "Show stored transcript"}
      </button>

      {isOpen ? (
        <div className="scroll-box">
          <p className="detail-text">{transcriptText}</p>
        </div>
      ) : (
        <p className="status-text">
          Transcript is saved and ready. Open it only when you want to inspect
          the raw text.
        </p>
      )}
    </div>
  );
}
