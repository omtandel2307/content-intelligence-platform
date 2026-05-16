"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { FormattedAiText } from "./formatted-ai-text";

type LibrarySource = {
  videoId: string;
  title: string;
  channelTitle: string;
  chunkIndex: number;
  score: number;
  textPreview: string;
};

type LibraryChatResponse = {
  answer: string;
  provider: string;
  model: string;
  messages: LibraryChatMessage[];
  sources: LibrarySource[];
};

export type LibraryChatMessage = {
  id: string;
  role: "USER" | "ASSISTANT";
  content: string;
  provider: string;
  model: string;
  createdAt: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
const visibleMessageCount = 6;

export function LibraryRagChatPanel({
  accountId,
  initialMessages,
  disabled,
}: {
  accountId: string;
  initialMessages: LibraryChatMessage[];
  disabled: boolean;
}) {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<LibraryChatMessage[]>(
    initialMessages
  );
  const [sources, setSources] = useState<LibrarySource[]>([]);
  const [showSources, setShowSources] = useState(false);
  const [showAllMessages, setShowAllMessages] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [isAsking, setIsAsking] = useState(false);

  async function askLibrary(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!message.trim()) {
      setError("Ask a question about your saved videos first.");
      return;
    }

    setError("");
    setStatus("Searching across your saved transcripts with local AI...");
    setIsAsking(true);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/accounts/${encodeURIComponent(
          accountId
        )}/library-rag/chat`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ message }),
        }
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(
          body?.message || body?.error || "Library chat request failed."
        );
      }

      const chat = body as LibraryChatResponse;
      setMessages(chat.messages || []);
      setSources(chat.sources || []);
      setShowSources(false);
      setShowAllMessages(false);
      setMessage("");
      setStatus(`Answered across your library with ${chat.provider}: ${chat.model}.`);
    } catch (exception) {
      setError(
        exception instanceof Error
          ? exception.message
          : "Library chat request failed."
      );
      setStatus("");
    } finally {
      setIsAsking(false);
    }
  }

  return (
    <div className="workspace-ai-card">
      <div className="stack">
        <p className="eyebrow">Library AI</p>
        <h3 className="section-title">Ask across all saved videos.</h3>
        <p className="body-copy">
          Ask one question and Ollama will retrieve matching transcript chunks
          from every saved video that has a transcript.
        </p>
      </div>

      <form className="stack" onSubmit={askLibrary}>
        <textarea
          className="text-area"
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          disabled={disabled || isAsking}
          placeholder={
            disabled
              ? "Select an account and save videos with transcripts first."
              : "Example: What have I learned about Spring Boot so far?"
          }
        />
        <button
          className={`button ${disabled ? "button-muted" : "button-accent"}`}
          type="submit"
          disabled={disabled || isAsking}
        >
          {isAsking ? "Thinking across library..." : "Ask library AI"}
        </button>
      </form>

      {status ? <p className="status-text">{status}</p> : null}
      {error ? <p className="status-warning">{error}</p> : null}

      {messages.length > 0 ? (
        <div className="chat-list">
          {messages.length > visibleMessageCount ? (
            <button
              className="button button-muted"
              type="button"
              onClick={() => setShowAllMessages((current) => !current)}
            >
              {showAllMessages
                ? "Show recent library messages only"
                : `Show ${messages.length - visibleMessageCount} older library messages`}
            </button>
          ) : null}

          {(showAllMessages
            ? messages
            : messages.slice(-visibleMessageCount)
          ).map((chatMessage) => (
            <div
              className={`chat-message ${
                chatMessage.role === "USER"
                  ? "chat-message-user"
                  : "chat-message-assistant"
              }`}
              key={chatMessage.id}
            >
              <p className="chat-author">
                {chatMessage.role === "USER"
                  ? "You"
                  : `Library AI - ${chatMessage.model}`}
              </p>
              {chatMessage.role === "ASSISTANT" ? (
                <FormattedAiText text={chatMessage.content} />
              ) : (
                <p className="detail-text">{chatMessage.content}</p>
              )}
            </div>
          ))}
        </div>
      ) : null}

      {sources.length > 0 ? (
        <div className="stack">
          <button
            className="button button-muted"
            type="button"
            onClick={() => setShowSources((current) => !current)}
          >
            {showSources ? "Hide library sources" : "Show library sources"}
          </button>

          {showSources ? (
            <div className="workspace-source-grid">
              {sources.map((source) => (
                <div
                  className="source-card"
                  key={`${source.videoId}-${source.chunkIndex}`}
                >
                  <Link className="source-title-link" href={`/video/${source.videoId}`}>
                    {source.title}
                  </Link>
                  <p className="chat-author">
                    {source.channelTitle} - chunk {source.chunkIndex} - score{" "}
                    {source.score}
                  </p>
                  <p className="video-description">{source.textPreview}</p>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}
