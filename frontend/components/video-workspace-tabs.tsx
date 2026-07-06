"use client";

import { useState } from "react";
import { FormattedAiText } from "./formatted-ai-text";
import { GenerateSummaryButton } from "./generate-summary-button";
import {
  ChatMessage,
  LocalRagChatPanel,
} from "./local-rag-chat-panel";
import { TranscriptTogglePanel } from "./transcript-toggle-panel";
import {
  QuizDetails,
  VideoQuizPanel,
} from "./video-quiz-panel";

type SummaryDetails = {
  videoId: string;
  summaryText: string | null;
  model: string | null;
  status: "READY" | "FAILED";
  failureReason: string | null;
  generatedAt: string;
};

type TranscriptDetails = {
  videoId: string;
  transcriptText: string | null;
  languageCode: string | null;
  status: "READY" | "UNAVAILABLE" | "FAILED";
  failureReason: string | null;
  fetchedAt: string;
};

type VideoTab = "summary" | "chat" | "quiz" | "transcript";

const tabs: Array<{
  id: VideoTab;
  label: string;
  description: string;
}> = [
  {
    id: "summary",
    label: "Summary",
    description: "Study notes",
  },
  {
    id: "chat",
    label: "Ask Video",
    description: "RAG chat",
  },
  {
    id: "quiz",
    label: "Quiz",
    description: "True/false test",
  },
  {
    id: "transcript",
    label: "Transcript",
    description: "Raw source text",
  },
];

export function VideoWorkspaceTabs({
  videoId,
  accountId,
  hasReadyTranscript,
  summary,
  transcript,
  chatHistory,
  quiz,
}: {
  videoId: string;
  accountId: string;
  hasReadyTranscript: boolean;
  summary: SummaryDetails | null;
  transcript: TranscriptDetails | null;
  chatHistory: ChatMessage[];
  quiz: QuizDetails | null;
}) {
  const [activeTab, setActiveTab] = useState<VideoTab>("summary");

  return (
    <section className="video-workspace-tools">
      <div className="video-tool-tabs" aria-label="Video workspace tools">
        {tabs.map((tab) => (
          <button
            className={`video-tool-tab ${
              activeTab === tab.id ? "video-tool-tab-active" : ""
            }`}
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            type="button"
          >
            <span>{tab.label}</span>
            <small>{tab.description}</small>
          </button>
        ))}
      </div>

      <div className="video-tool-panel">
        {activeTab === "summary" ? (
          <section className="panel">
            <div className="video-panel-header">
              <div className="stack">
                <p className="eyebrow">Summary</p>
                <h2 className="section-title">
                  {summary?.status === "READY"
                    ? "Generated study notes"
                    : summary?.status === "FAILED"
                      ? "Summary failed"
                      : "Create a focused summary"}
                </h2>
              </div>
              <GenerateSummaryButton
                videoId={videoId}
                disabled={!hasReadyTranscript}
              />
            </div>

            {summary?.status === "READY" && summary.summaryText ? (
              <FormattedAiText text={summary.summaryText} />
            ) : (
              <p className="body-copy">
                {summary?.failureReason ||
                  "Fetch the transcript first, then generate concise study notes for this video."}
              </p>
            )}
          </section>
        ) : null}

        {activeTab === "chat" ? (
          <LocalRagChatPanel
            videoId={videoId}
            accountId={accountId}
            disabled={!hasReadyTranscript}
            initialMessages={chatHistory}
          />
        ) : null}

        {activeTab === "quiz" ? (
          <VideoQuizPanel
            videoId={videoId}
            disabled={!hasReadyTranscript}
            initialQuiz={quiz}
          />
        ) : null}

        {activeTab === "transcript" ? (
          <section className="panel">
            <div className="stack">
              <p className="eyebrow">Transcript</p>
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
                  "Fetch the transcript to prepare this video for summaries, quizzes, and chat."}
              </p>
            )}
          </section>
        ) : null}
      </div>
    </section>
  );
}
