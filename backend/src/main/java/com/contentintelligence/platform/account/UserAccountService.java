package com.contentintelligence.platform.account;

import com.contentintelligence.platform.account.dto.AccountResponse;
import com.contentintelligence.platform.account.dto.CreateAccountRequest;
import com.contentintelligence.platform.account.dto.WorkspaceChatResponse;
import com.contentintelligence.platform.account.dto.WorkspaceResponse;
import com.contentintelligence.platform.account.dto.WorkspaceSavedVideoResponse;
import com.contentintelligence.platform.content.AccountSavedContent;
import com.contentintelligence.platform.content.AccountSavedContentRepository;
import com.contentintelligence.platform.content.ContentItem;
import com.contentintelligence.platform.content.ContentItemRepository;
import com.contentintelligence.platform.rag.RagChatMessage;
import com.contentintelligence.platform.rag.RagChatMessageRepository;
import com.contentintelligence.platform.rag.RagChatRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class UserAccountService {

    private final UserAccountRepository accountRepository;
    private final AccountSavedContentRepository savedContentRepository;
    private final ContentItemRepository contentItemRepository;
    private final RagChatMessageRepository chatMessageRepository;

    public UserAccountService(
            UserAccountRepository accountRepository,
            AccountSavedContentRepository savedContentRepository,
            ContentItemRepository contentItemRepository,
            RagChatMessageRepository chatMessageRepository
    ) {
        this.accountRepository = accountRepository;
        this.savedContentRepository = savedContentRepository;
        this.contentItemRepository = contentItemRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public List<AccountResponse> listAccounts() {
        return accountRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse createAccount(CreateAccountRequest request) {
        UserAccount account = new UserAccount(
                request.displayName().trim(),
                request.email() == null || request.email().isBlank()
                        ? null
                        : request.email().trim()
        );
        return AccountResponse.from(accountRepository.save(account));
    }

    public UserAccount requireAccount(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Select an account before using this feature."
            );
        }

        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Selected account was not found."
                ));
    }

    public WorkspaceResponse getWorkspace(String accountId) {
        UserAccount account = requireAccount(accountId);
        List<AccountSavedContent> savedItems =
                savedContentRepository.findAllByAccountIdOrderBySavedAtDesc(accountId);
        Map<String, ContentItem> contentByVideoId = contentItemRepository
                .findAllById(savedItems.stream().map(AccountSavedContent::getVideoId).toList())
                .stream()
                .collect(Collectors.toMap(ContentItem::getVideoId, Function.identity()));
        List<WorkspaceSavedVideoResponse> recentVideos = savedItems.stream()
                .limit(3)
                .map(saved -> {
                    ContentItem item = contentByVideoId.get(saved.getVideoId());
                    if (item == null) {
                        return null;
                    }

                    return new WorkspaceSavedVideoResponse(
                            item.getVideoId(),
                            item.getTitle(),
                            item.getChannelTitle(),
                            item.getThumbnailUrl(),
                            saved.getSavedAt()
                    );
                })
                .filter(item -> item != null)
                .toList();
        List<RagChatMessage> recentMessages =
                chatMessageRepository.findTop5ByAccountIdAndRoleOrderByCreatedAtDesc(
                        accountId,
                        RagChatRole.USER
                );
        List<WorkspaceChatResponse> recentChats = recentMessages.stream()
                .map(message -> {
                    ContentItem item = contentByVideoId.get(message.getVideoId());
                    return new WorkspaceChatResponse(
                            message.getVideoId(),
                            item == null ? "Saved video" : item.getTitle(),
                            message.getRole().name(),
                            preview(message.getContent()),
                            message.getCreatedAt()
                    );
                })
                .toList();

        return new WorkspaceResponse(
                AccountResponse.from(account),
                (int) savedContentRepository.countByAccountId(accountId),
                (int) chatMessageRepository.countByAccountId(accountId),
                recentVideos,
                recentChats
        );
    }

    private String preview(String content) {
        int maxLength = 140;

        if (content.length() <= maxLength) {
            return content;
        }

        return content.substring(0, maxLength).trim() + "...";
    }
}
