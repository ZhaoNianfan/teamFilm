package com.myself.teamfiles.module.ai.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.ai.dto.AiConfigDTO;
import com.myself.teamfiles.module.ai.dto.ChatRequestDTO;
import com.myself.teamfiles.module.ai.entity.AiApiConfig;
import com.myself.teamfiles.module.ai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // ==================== Config ====================

    @OperationLog(module = "AI", operation = "Create AI config")
    @PostMapping("/config")
    public R<AiApiConfig> createConfig(@Valid @RequestBody AiConfigDTO dto) {
        return R.ok(aiService.createConfig(dto));
    }

    @OperationLog(module = "AI", operation = "Update AI config")
    @PutMapping("/config/{id}")
    public R<AiApiConfig> updateConfig(@PathVariable Long id, @Valid @RequestBody AiConfigDTO dto) {
        return R.ok(aiService.updateConfig(id, dto));
    }

    @OperationLog(module = "AI", operation = "Delete AI config")
    @DeleteMapping("/config/{id}")
    public R<Void> deleteConfig(@PathVariable Long id) {
        aiService.deleteConfig(id);
        return R.ok();
    }

    @GetMapping("/config")
    public R<List<AiApiConfig>> getAvailableConfigs() {
        return R.ok(aiService.getAvailableConfigs());
    }

    @OperationLog(module = "AI", operation = "Test AI connection")
    @PostMapping("/config/{id}/test")
    public R<Map<String, Object>> testConnection(@PathVariable Long id) {
        return R.ok(aiService.testConnection(id));
    }

    // ==================== Admin AI Config ====================

    @OperationLog(module = "AI", operation = "Admin: Create global AI config")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/admin/config")
    public R<AiApiConfig> createSystemConfig(@Valid @RequestBody AiConfigDTO dto) {
        dto.setSystemConfig(true);
        return R.ok(aiService.createConfig(dto));
    }

    @OperationLog(module = "AI", operation = "Admin: Update global AI config")
    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/admin/config/{id}")
    public R<AiApiConfig> updateSystemConfig(@PathVariable Long id, @Valid @RequestBody AiConfigDTO dto) {
        return R.ok(aiService.updateConfig(id, dto));
    }

    // ==================== Chat ====================

    @OperationLog(module = "AI", operation = "Get file summary")
    @PostMapping("/chat/summary")
    public R<String> summarizeFile(@RequestParam Long fileId) {
        return R.ok(aiService.summarizeFile(fileId));
    }

    @PostMapping("/chat/qa")
    public R<Map<String, String>> chatSync(@Valid @RequestBody ChatRequestDTO dto) {
        return R.ok(aiService.chatSync(dto.getMessage(), dto.getConversationId(), dto.getFileIds()));
    }

    @PostMapping(value = "/chat/qa/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@Valid @RequestBody ChatRequestDTO dto) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout
        aiService.chat(dto.getMessage(), dto.getConversationId(), dto.getFileIds(),
                token -> {
                    try { emitter.send(SseEmitter.event().data(token)); } catch (Exception ignored) {}
                },
                () -> {
                    try { emitter.send(SseEmitter.event().data("[DONE]")); emitter.complete(); } catch (Exception ignored) {}
                },
                error -> {
                    try { emitter.send(SseEmitter.event().data("错误: " + error.getMessage())); emitter.complete(); } catch (Exception ignored) {}
                });
        return emitter;
    }

    // ==================== Conversations ====================

    @GetMapping("/chat/conversations")
    public R<List<Map<String, Object>>> getConversations() {
        return R.ok(aiService.getConversations());
    }

    @GetMapping("/chat/conversations/{id}")
    public R<Map<String, Object>> getConversation(@PathVariable String id) {
        return R.ok(aiService.getConversation(id));
    }

    @OperationLog(module = "AI", operation = "Delete conversation")
    @DeleteMapping("/chat/conversations/{id}")
    public R<Void> deleteConversation(@PathVariable String id) {
        aiService.deleteConversation(id);
        return R.ok();
    }
}
