"use client";

import { useState } from "react";
import {
  LibraryChatMessage,
  LibraryRagChatPanel,
} from "./library-rag-chat-panel";
import { KnowledgeMapPanel, type KnowledgeMap } from "./knowledge-map-panel";
import { CompareVideosPanel } from "./compare-videos-panel";
import { LearningTimelinePanel } from "./learning-timeline-panel";
import { ProjectBuilderPanel } from "./project-builder-panel";

type WorkspaceTab = "chat" | "map" | "compare" | "project" | "timeline";

const tabs: Array<{
  id: WorkspaceTab;
  label: string;
  description: string;
}> = [
  {
    id: "chat",
    label: "Library Chat",
    description: "Ask across saved transcripts",
  },
  {
    id: "map",
    label: "Learning Map",
    description: "Topics and connections",
  },
  {
    id: "compare",
    label: "Compare",
    description: "Two-video study guide",
  },
  {
    id: "project",
    label: "Project Plan",
    description: "Turn learning into a build",
  },
  {
    id: "timeline",
    label: "Timeline",
    description: "Recent learning activity",
  },
];

export function WorkspaceToolsTabs({
  accountId,
  savedVideoCount,
  initialMessages,
}: {
  accountId: string;
  savedVideoCount: number;
  initialMessages: LibraryChatMessage[];
}) {
  const [activeTab, setActiveTab] = useState<WorkspaceTab>("chat");
  const [knowledgeMap, setKnowledgeMap] = useState<KnowledgeMap | null>(null);
  const hasSavedVideos = savedVideoCount > 0;

  return (
    <section className="workspace-tools">
      <div className="workspace-tools-tabs" aria-label="Workspace tools">
        {tabs.map((tab) => (
          <button
            className={`workspace-tool-tab ${
              activeTab === tab.id ? "workspace-tool-tab-active" : ""
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

      <div className="workspace-tool-panel">
        {activeTab === "chat" ? (
          <LibraryRagChatPanel
            accountId={accountId}
            initialMessages={initialMessages}
            disabled={!hasSavedVideos}
          />
        ) : null}

        {activeTab === "map" ? (
          <KnowledgeMapPanel 
          accountId={accountId} 
          disabled={!hasSavedVideos} 
          knowledgeMap={knowledgeMap}
          setKnowledgeMap={setKnowledgeMap} />
        ) : null}

        {activeTab === "compare" ? (
          <CompareVideosPanel
            accountId={accountId}
            disabled={savedVideoCount < 2}
          />
        ) : null}

        {activeTab === "project" ? (
          <ProjectBuilderPanel accountId={accountId} disabled={false} />
        ) : null}

        {activeTab === "timeline" ? (
          <LearningTimelinePanel accountId={accountId} disabled={!hasSavedVideos} />
        ) : null}
      </div>
    </section>
  );
}
