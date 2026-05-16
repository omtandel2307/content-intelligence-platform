"use client";

import Link from "next/link";
import type { CSSProperties } from "react";
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

type KnowledgeMap = {
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
}: {
  accountId: string;
  disabled: boolean;
}) {
  const [knowledgeMap, setKnowledgeMap] = useState<KnowledgeMap | null>(null);
  const [selectedNodeId, setSelectedNodeId] = useState("");
  const [status, setStatus] = useState("");
  const [error, setError] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);

  async function generateMap() {
    setError("");
    setStatus("Asking Ollama to extract your learning map...");
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
      setSelectedNodeId(map.nodes[0]?.id || "");
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

  const selectedNode =
    knowledgeMap?.nodes.find((node) => node.id === selectedNodeId) ||
    knowledgeMap?.nodes[0];
  const nodeById = new Map(
    (knowledgeMap?.nodes || []).map((node) => [node.id, node])
  );

  return (
    <div className="knowledge-map-card">
      <div className="workspace-header">
        <div className="stack">
          <p className="eyebrow">AI Knowledge Map</p>
          <h3 className="section-title">See what your saved videos teach.</h3>
          <p className="body-copy">
            Ollama extracts core topics from saved transcripts and connects how
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
        <div className="knowledge-map-layout">
          <div className="knowledge-node-cloud">
            {knowledgeMap.nodes.map((node) => (
              <button
                className={`knowledge-node ${
                  node.id === selectedNode?.id ? "knowledge-node-active" : ""
                }`}
                key={node.id}
                onClick={() => setSelectedNodeId(node.id)}
                style={
                  {
                    "--node-weight": node.importance,
                  } as CSSProperties & Record<"--node-weight", number>
                }
                type="button"
              >
                <span>{node.label}</span>
                <small>Importance {node.importance}/5</small>
              </button>
            ))}
          </div>

          <div className="knowledge-detail">
            {selectedNode ? (
              <>
                <p className="eyebrow">Selected Topic</p>
                <h4>{selectedNode.label}</h4>
                <p className="body-copy">{selectedNode.summary}</p>
                {selectedNode.videoIds.length > 0 ? (
                  <div className="topic-video-links">
                    {selectedNode.videoIds.map((videoId) => (
                      <Link
                        className="surface-chip"
                        href={`/video/${videoId}`}
                        key={videoId}
                      >
                        Open video {videoId}
                      </Link>
                    ))}
                  </div>
                ) : (
                  <p className="status-text">
                    Ollama did not attach a specific saved video to this topic.
                  </p>
                )}
              </>
            ) : null}
          </div>

          {knowledgeMap.links.length > 0 ? (
            <div className="knowledge-links">
              <p className="detail-label">Topic relationships</p>
              {knowledgeMap.links.map((link, index) => (
                <div className="knowledge-link-row" key={`${link.source}-${link.target}-${index}`}>
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
