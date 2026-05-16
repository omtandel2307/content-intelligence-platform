import Link from "next/link";
import {
  LibraryChatMessage,
  LibraryRagChatPanel,
} from "./library-rag-chat-panel";
import { KnowledgeMapPanel } from "./knowledge-map-panel";
import { CompareVideosPanel } from "./compare-videos-panel";
import { LearningTimelinePanel } from "./learning-timeline-panel";
import { ProjectBuilderPanel } from "./project-builder-panel";

type WorkspaceSavedVideo = {
  videoId: string;
  title: string;
  channelTitle: string;
  thumbnailUrl: string | null;
  savedAt: string;
};

type WorkspaceChat = {
  videoId: string;
  title: string;
  role: string;
  contentPreview: string;
  createdAt: string;
};

type Workspace = {
  account: {
    id: string;
    displayName: string;
    email: string | null;
    createdAt: string;
  };
  savedVideoCount: number;
  chatMessageCount: number;
  recentVideos: WorkspaceSavedVideo[];
  recentChats: WorkspaceChat[];
};

const apiBaseUrl =
  process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

export async function MyWorkspacePanel({ accountId }: { accountId: string }) {
  if (!accountId) {
    return (
      <section className="empty-state">
        <p className="eyebrow">My Workspace</p>
        <h2 className="section-title">Create an account to start saving work.</h2>
        <p className="body-copy">
          Your library, video notes, and local AI chats will appear here once an
          account is selected.
        </p>
      </section>
    );
  }

  const workspace = await getWorkspace(accountId);
  const libraryChatMessages = await getLibraryChatMessages(accountId);

  if (!workspace) {
    return (
      <section className="empty-state">
        <p className="eyebrow">My Workspace</p>
        <h2 className="section-title">Workspace unavailable.</h2>
        <p className="body-copy">
          The selected account could not be loaded. Try switching accounts or
          restarting the backend.
        </p>
      </section>
    );
  }

  return (
    <section className="workspace-dashboard">
      <div className="workspace-header">
        <div className="stack">
          <p className="eyebrow">My Workspace</p>
          <h2 className="section-title">{workspace.account.displayName}</h2>
          <p className="body-copy">
            Continue where this account left off.
          </p>
        </div>
        <div className="workspace-stats">
          <Stat label="Saved" value={workspace.savedVideoCount.toString()} />
          <Stat label="Chats" value={workspace.chatMessageCount.toString()} />
        </div>
      </div>

      <div className="workspace-feature-grid">
        <LibraryRagChatPanel
          accountId={accountId}
          initialMessages={libraryChatMessages}
          disabled={workspace.savedVideoCount === 0}
        />

        <KnowledgeMapPanel
          accountId={accountId}
          disabled={workspace.savedVideoCount === 0}
        />

        <CompareVideosPanel
          accountId={accountId}
          disabled={workspace.savedVideoCount < 2}
        />

        <ProjectBuilderPanel
          accountId={accountId}
          disabled={workspace.savedVideoCount === 0}
        />
      </div>

      <LearningTimelinePanel
        accountId={accountId}
        disabled={workspace.savedVideoCount === 0}
      />

      <div className="workspace-grid">
        <div className="stack">
          <div className="meta-row">
            <p className="detail-label">Recent videos</p>
            <Link className="surface-chip" href="/library">
              Open library
            </Link>
          </div>

          {workspace.recentVideos.length > 0 ? (
            workspace.recentVideos.map((video) => (
              <Link
                className="workspace-video-row"
                href={`/video/${video.videoId}`}
                key={video.videoId}
              >
                {video.thumbnailUrl ? (
                  <img src={video.thumbnailUrl} alt={video.title} />
                ) : (
                  <div />
                )}
                <span>
                  <strong>{video.title}</strong>
                  <small>{video.channelTitle}</small>
                </span>
              </Link>
            ))
          ) : (
            <p className="status-text">
              No saved videos yet. Search above and save your first one.
            </p>
          )}
        </div>

        <div className="stack">
          <p className="detail-label">Recent chats</p>
          {workspace.recentChats.length > 0 ? (
            workspace.recentChats.map((chat) => (
              <Link
                className="workspace-chat-row"
                href={`/video/${chat.videoId}`}
                key={`${chat.videoId}-${chat.createdAt}`}
              >
                <strong>{chat.title}</strong>
                <span>You asked: {chat.contentPreview}</span>
              </Link>
            ))
          ) : (
            <p className="status-text">
              No saved chat history yet. Ask a question on any video workspace.
            </p>
          )}
        </div>
      </div>
    </section>
  );
}

async function getWorkspace(accountId: string) {
  try {
    const response = await fetch(
      `${apiBaseUrl}/api/accounts/${encodeURIComponent(accountId)}/workspace`,
      { cache: "no-store" }
    );

    if (!response.ok) {
      return null;
    }

    return (await response.json()) as Workspace;
  } catch {
    return null;
  }
}

async function getLibraryChatMessages(accountId: string) {
  try {
    const response = await fetch(
      `${apiBaseUrl}/api/accounts/${encodeURIComponent(
        accountId
      )}/library-rag/chat`,
      { cache: "no-store" }
    );

    if (!response.ok) {
      return [];
    }

    return (await response.json()) as LibraryChatMessage[];
  } catch {
    return [];
  }
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="metric-card">
      <p className="metric-label">{label}</p>
      <p className="metric-value">{value}</p>
    </div>
  );
}
