"use client";

import Link from "next/link";
import { useState } from "react";

type KnowledgeNode = {
  id: string;
  label: string;
  summary: string;
  importance: number;
  videoIds: string[];
};

type KnowledgeLink = {
  source: string;
  target: string;
  relation: string;
  strength: number;
};

export type KnowledgeMap = {
  accountId: string;
  provider: string;
  model: string;
  generatedAt: string;
  nodes: KnowledgeNode[];
  links: KnowledgeLink[];
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export function KnowledgeMapPanel({
  accountId,
  disabled,
  knowledgeMap,
  setKnowledgeMap,
}: {
  accountId: string;
  disabled: boolean;
  knowledgeMap: KnowledgeMap | null;
  setKnowledgeMap: (map: KnowledgeMap | null) => void;
}) {
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);

  async function generateMap() {
    setError("");
    setStatus("Asking OpenAI to extract your learning map...");
    setIsGenerating(true);

    try {
      const response = await fetch(
        `${apiBaseUrl}/api/accounts/${encodeURIComponent(
          accountId
        )}/knowledge-map`,
        { method: "POST" }
      );
      const body = await response.json();

      if (!response.ok) {
        throw new Error(
          body?.message || body?.error || "Knowledge map generation failed."
        );
      }

      const map = body as KnowledgeMap;
      setKnowledgeMap(map);
      setStatus(`Generated with ${map.provider}: ${map.model}.`);
    } catch (exception) {
      setKnowledgeMap(null);
      setError(
        exception instanceof Error
          ? exception.message
          : "Knowledge map generation failed."
      );
      setStatus("");
    } finally {
      setIsGenerating(false);
    }
  }

  const sortedNodes = [...(knowledgeMap?.nodes || [])].sort(
    (first, second) => second.importance - first.importance
  );
  const nodeById = new Map(
    (knowledgeMap?.nodes || []).map((node) => [node.id, node])
  );
  const focusPath = sortedNodes.slice(0, 5);

  return (
    <div className="knowledge-map-card">
      <div className="workspace-header">
        <div className="stack">
          <p className="eyebrow">AI Knowledge Map</p>
          <h3 className="section-title">See what your saved videos teach.</h3>
          <p className="body-copy">
            AI extracts core topics from saved transcripts and connects how
            those ideas relate.
          </p>
        </div>
        <button
          className={`button ${disabled ? "button-muted" : "button-dark"}`}
          type="button"
          onClick={generateMap}
          disabled={disabled || isGenerating}
        >
          {isGenerating ? "Mapping..." : "Generate map"}
        </button>
      </div>

      {status ? <p className="status-text">{status}</p> : null}
      {error ? <p className="status-warning">{error}</p> : null}

      {knowledgeMap ? (
        <div className="learning-map-layout">
          <section className="learning-map-section">
            <div className="learning-map-section-header">
              <div>
                <p className="detail-label">Core topics</p>
                <p className="status-text">
                  Ordered by how central each topic appears across your saved
                  transcripts.
                </p>
              </div>
              <span className="surface-chip">{knowledgeMap.nodes.length} topics</span>
            </div>

            <div className="learning-topic-grid">
              {sortedNodes.map((node) => (
                <article className="learning-topic-card" key={node.id}>
                  <div className="meta-row">
                    <h4>{node.label}</h4>
                    <span className="importance-badge">{node.importance}/5</span>
                  </div>
                  <p className="video-description">{node.summary}</p>
                  {node.videoIds.length > 0 ? (
                    <div className="topic-video-links">
                      {node.videoIds.slice(0, 4).map((videoId) => (
                        <Link
                          className="surface-chip"
                          href={`/video/${videoId}`}
                          key={videoId}
                        >
                          Video {videoId}
                        </Link>
                      ))}
                    </div>
                  ) : (
                    <p className="status-text">No source video was attached.</p>
                  )}
                </article>
              ))}
            </div>
          </section>

          {focusPath.length > 0 ? (
            <section className="learning-map-section">
              <p className="detail-label">Suggested focus order</p>
              <div className="learning-path-list">
                {focusPath.map((node, index) => (
                  <div className="learning-path-row" key={node.id}>
                    <span>{index + 1}</span>
                    <strong>{node.label}</strong>
                    <small>{node.summary}</small>
                  </div>
                ))}
              </div>
            </section>
          ) : null}

          {knowledgeMap.links.length > 0 ? (
            <section className="learning-map-section">
              <p className="detail-label">Topic connections</p>
              <div className="knowledge-links-list">
                {knowledgeMap.links.map((link, index) => (
                  <div
                    className="knowledge-link-row"
                    key={`${link.source}-${link.target}-${index}`}
                  >
                    <strong>
                      {nodeById.get(link.source)?.label || link.source}
                      <span>{" -> "}</span>
                      {nodeById.get(link.target)?.label || link.target}
                    </strong>
                    <small>
                      {link.relation || "Related concept"} - strength{" "}
                      {link.strength}/5
                    </small>
                  </div>
                ))}
              </div>
            </section>
          ) : null}
        </div>
      ) : (
        <p className="status-text">
          Generate a map after saving videos and fetching transcripts.
        </p>
      )}
    </div>
  );
}
