import Link from "next/link";
import {
  LibraryChatMessage,
} from "./library-rag-chat-panel";
import { WorkspaceToolsTabs } from "./workspace-tools-tabs";

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
          Your library, video notes, and AI chats will appear here once an
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

      <div className="workspace-main-layout">
        <WorkspaceToolsTabs
          accountId={accountId}
          savedVideoCount={workspace.savedVideoCount}
          initialMessages={libraryChatMessages}
        />

        <aside className="workspace-activity-rail">
          <RecentVideos videos={workspace.recentVideos} />
          <RecentChats chats={workspace.recentChats} />
        </aside>
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

function RecentVideos({ videos }: { videos: WorkspaceSavedVideo[] }) {
  return (
    <div className="workspace-activity-card">
      <div className="meta-row">
        <p className="detail-label">Recent videos</p>
        <Link className="surface-chip" href="/library">
          Open library
        </Link>
      </div>

      {videos.length > 0 ? (
        videos.slice(0, 4).map((video) => (
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
          No saved videos yet. Search and save a video to start the workspace.
        </p>
      )}
    </div>
  );
}

function RecentChats({ chats }: { chats: WorkspaceChat[] }) {
  return (
    <div className="workspace-activity-card">
      <p className="detail-label">Recent chats</p>

      {chats.length > 0 ? (
        chats.slice(0, 4).map((chat) => (
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
          Ask a question on any video workspace to build chat history.
        </p>
      )}
    </div>
  );
}
