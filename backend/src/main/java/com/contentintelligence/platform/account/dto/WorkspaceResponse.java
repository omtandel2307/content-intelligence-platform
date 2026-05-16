package com.contentintelligence.platform.account.dto;

import java.util.List;

public record WorkspaceResponse(
        AccountResponse account,
        int savedVideoCount,
        int chatMessageCount,
        List<WorkspaceSavedVideoResponse> recentVideos,
        List<WorkspaceChatResponse> recentChats
) {
}
