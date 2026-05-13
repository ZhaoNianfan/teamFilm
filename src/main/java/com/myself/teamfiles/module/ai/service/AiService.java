package com.myself.teamfiles.module.ai.service;

import com.myself.teamfiles.module.ai.dto.AiConfigDTO;
import com.myself.teamfiles.module.ai.entity.AiApiConfig;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface AiService {
    // Config
    AiApiConfig createConfig(AiConfigDTO dto);
    AiApiConfig updateConfig(Long id, AiConfigDTO dto);
    void deleteConfig(Long id);
    List<AiApiConfig> getAvailableConfigs();
    Map<String, Object> testConnection(Long configId);

    // Chat streaming (deprecated, use chatSync)
    void chat(String message, String conversationId, List<Long> fileIds,
              Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError);

    // Chat non-streaming: returns {conversationId, reply}
    Map<String, String> chatSync(String message, String conversationId, List<Long> fileIds);

    // Summary
    String summarizeFile(Long fileId);

    // Conversations
    List<Map<String, Object>> getConversations();
    Map<String, Object> getConversation(String id);
    void deleteConversation(String id);
}
