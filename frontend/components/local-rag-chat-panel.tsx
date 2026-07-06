"use client";

import { FormEvent, useState } from "react";
import { FormattedAiText } from "./formatted-ai-text";
import { accountHeaders } from "./account-switcher";

type RagSource = {
  chunkIndex: number;
  score: number;
  textPreview: string;
};

type LocalChatResponse = {
  videoId: string;
  answer: string;
  provider: string;
  model: string;
  messages: ChatMessage[];
  sources: RagSource[];
};

export type ChatMessage = {
  id: string;
  videoId: string;
  role: "USER" | "ASSISTANT";
  content: string;
  provider: string;
  model: string;
  createdAt: string;
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
const visibleMessageCount = 8;

export function LocalRagChatPanel({
  videoId,
  accountId,
  disabled,
  initialMessages,
}: {
  videoId: string;
  accountId: string;
  disabled: boolean;
  initialMessages: ChatMessage[];
}) {
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>(initialMessages);
  const [sources, setSources] = useState<RagSource[]>([]);
  const [showSources, setShowSources] = useState(false);
  const [showAllMessages, setShowAllMessages] = useState(false);
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [isIndexing, setIsIndexing] = useState(false);
  const [isAsking, setIsAsking] = useState(false);

  async function prepareIndex() {
    setError("");
    setStatus("Building RAG index with OpenAI embeddings...");
    setIsIndexing(true);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/rag/index`,
        { method: "POST" }
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(body?.message || body?.error || "RAG indexing failed.");
      }

      setStatus(
        `RAG ready: ${body.chunkCount} chunks indexed with ${body.embeddingModel}.`
      );
    } catch (exception) {
      setError(
        exception instanceof Error
          ? exception.message
          : "RAG indexing failed."
      );
      setStatus("");
    } finally {
      setIsIndexing(false);
    }
  }

  async function askQuestion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (!message.trim()) {
      setError("Ask a question first.");
      return;
    }

    setError("");
    setStatus("Asking OpenAI...");
    setIsAsking(true);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/content/${encodeURIComponent(videoId)}/rag/chat`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            ...accountHeaders(accountId),
          },
          body: JSON.stringify({ message }),
        }
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(body?.message || body?.error || "Local chat failed.");
      }

      const chat = body as LocalChatResponse;
      setMessages(chat.messages || []);
      setSources(chat.sources || []);
      setShowSources(false);
      setShowAllMessages(false);
      setMessage("");
      setStatus(`Answered with ${chat.provider}: ${chat.model}.`);
    } catch (exception) {
      setError(
        exception instanceof Error ? exception.message : "AI chat failed."
      );
      setStatus("");
    } finally {
      setIsAsking(false);
    }
  }

  return (
    <section className="panel">
      <div className="stack">
        <p className="eyebrow">RAG Chat</p>
        <h2 className="section-title">Ask this video with OpenAI.</h2>
        <p className="body-copy">
          OpenAI answers after retrieving the most relevant transcript chunks
          from Postgres.
        </p>
      </div>

      <button
        className={`button ${disabled ? "button-muted" : "button-dark"}`}
        type="button"
        onClick={prepareIndex}
        disabled={disabled || isIndexing || isAsking}
      >
        {isIndexing ? "Preparing RAG..." : "Prepare RAG index"}
      </button>

      <form className="stack" onSubmit={askQuestion}>
        <textarea
          className="text-area"
          value={message}
          onChange={(event) => setMessage(event.target.value)}
          disabled={disabled || isAsking || isIndexing}
          placeholder={
            disabled
              ? "Fetch the transcript first."
              : "Example: What are the main ideas in this video?"
          }
        />
        <button
          className={`button ${disabled ? "button-muted" : "button-accent"}`}
          type="submit"
          disabled={disabled || isAsking || isIndexing}
        >
          {isAsking ? "Thinking..." : "Ask AI"}
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
                ? "Show recent messages only"
                : `Show ${messages.length - visibleMessageCount} older messages`}
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
                  : `AI - ${chatMessage.model}`}
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
            {showSources ? "Hide retrieved chunks" : "Show retrieved chunks"}
          </button>

          {showSources ? (
            <div className="stack">
              <p className="eyebrow">Retrieved Sources</p>
              {sources.map((source) => (
                <div className="source-card" key={source.chunkIndex}>
                  <p className="metric-value">
                    Chunk {source.chunkIndex} - score {source.score}
                  </p>
                  <p className="video-description">{source.textPreview}</p>
                </div>
              ))}
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
