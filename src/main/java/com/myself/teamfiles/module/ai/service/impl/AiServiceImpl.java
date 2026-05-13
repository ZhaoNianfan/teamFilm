package com.myself.teamfiles.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myself.teamfiles.common.exception.BusinessException;
import com.myself.teamfiles.common.exception.ErrorCode;
import com.myself.teamfiles.common.util.EncryptUtil;
import com.myself.teamfiles.module.ai.dto.AiConfigDTO;
import com.myself.teamfiles.module.ai.entity.AiApiConfig;
import com.myself.teamfiles.module.ai.mapper.AiApiConfigMapper;
import com.myself.teamfiles.module.ai.service.AiService;
import com.myself.teamfiles.module.file.entity.FileInfo;
import com.myself.teamfiles.module.file.mapper.FileInfoMapper;
import com.myself.teamfiles.security.JwtContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final AiApiConfigMapper aiApiConfigMapper;
    private final FileInfoMapper fileInfoMapper;
    private final ObjectMapper objectMapper;
    private final Map<String, List<Map<String, Object>>> conversationStore = new ConcurrentHashMap<>();

    @Value("${file.storage.base-path}")
    private String basePath;

    public AiServiceImpl(AiApiConfigMapper aiApiConfigMapper, FileInfoMapper fileInfoMapper) {
        this.aiApiConfigMapper = aiApiConfigMapper;
        this.fileInfoMapper = fileInfoMapper;
        this.objectMapper = new ObjectMapper();
    }

    // ==================== Config ====================

    @Override
    @Transactional
    public AiApiConfig createConfig(AiConfigDTO dto) {
        Long userId = JwtContextHolder.getUserId();
        AiApiConfig config = new AiApiConfig();
        config.setUserId(dto.isSystemConfig() ? null : userId);
        config.setIsSystem(dto.isSystemConfig() ? 1 : 0);
        config.setApiType(dto.getApiType().toUpperCase());
        config.setApiKey(EncryptUtil.encrypt(dto.getApiKey()));
        config.setApiBaseUrl(dto.getApiBaseUrl());
        config.setModelName(dto.getModelName() != null ? dto.getModelName() : "gpt-3.5-turbo");
        config.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : 1);
        config.setTested(0);
        aiApiConfigMapper.insert(config);
        config.setApiKey(null);
        return config;
    }

    @Override
    @Transactional
    public AiApiConfig updateConfig(Long id, AiConfigDTO dto) {
        AiApiConfig config = aiApiConfigMapper.selectById(id);
        if (config == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        if (dto.getApiType() != null) config.setApiType(dto.getApiType().toUpperCase());
        if (dto.getApiKey() != null) config.setApiKey(EncryptUtil.encrypt(dto.getApiKey()));
        if (dto.getApiBaseUrl() != null) config.setApiBaseUrl(dto.getApiBaseUrl());
        if (dto.getModelName() != null) config.setModelName(dto.getModelName());
        if (dto.getIsActive() != null) config.setIsActive(dto.getIsActive());
        aiApiConfigMapper.updateById(config);
        config.setApiKey(null);
        return config;
    }

    @Override
    @Transactional
    public void deleteConfig(Long id) { aiApiConfigMapper.deleteById(id); }

    @Override
    public List<AiApiConfig> getAvailableConfigs() {
        Long userId = JwtContextHolder.getUserId();
        List<AiApiConfig> configs = aiApiConfigMapper.selectList(
                new LambdaQueryWrapper<AiApiConfig>().eq(AiApiConfig::getIsActive, 1)
                        .and(w -> w.eq(AiApiConfig::getUserId, userId).or().eq(AiApiConfig::getIsSystem, 1)));
        configs.forEach(c -> c.setApiKey(null));
        return configs;
    }

    @Override
    public Map<String, Object> testConnection(Long configId) {
        AiApiConfig config = aiApiConfigMapper.selectById(configId);
        if (config == null) throw new BusinessException(ErrorCode.NOT_FOUND);
        Map<String, Object> result = new HashMap<>();
        try {
            String apiKey = EncryptUtil.decrypt(config.getApiKey());
            String baseUrl = getBaseUrl(config);
            Map<String, Object> body = Map.of("model", config.getModelName() != null ? config.getModelName() : "gpt-3.5-turbo",
                    "messages", List.of(Map.of("role", "user", "content", "Hi, reply OK only.")), "max_tokens", 10);
            httpPost(baseUrl + "/chat/completions", apiKey, body);
            result.put("success", true);
            config.setTested(1);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            config.setTested(0);
        }
        aiApiConfigMapper.updateById(config);
        return result;
    }

    // ==================== Chat ====================

    @Override
    public void chat(String message, String conversationId, List<Long> fileIds,
                     Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        AiApiConfig config = getBestConfig();
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();

        if (config == null) {
            onToken.accept("错误：未配置 AI 服务，请先在系统设置或 AI 配置中添加 API Key。");
            onComplete.run();
            return;
        }

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                String apiKey = EncryptUtil.decrypt(config.getApiKey());
                String baseUrl = getBaseUrl(config);
                String model = config.getModelName() != null ? config.getModelName() : "gpt-3.5-turbo";

                List<Map<String, Object>> msgs = new ArrayList<>();
                msgs.add(Map.of("role", "system", "content", buildSystemPrompt(fileIds)));
                List<Map<String, Object>> hist = conversationStore.computeIfAbsent(convId, k -> new ArrayList<>());
                int s = Math.max(0, hist.size() - 10);
                for (int i = s; i < hist.size(); i++) {
                    Map<String, Object> m = hist.get(i);
                    msgs.add(Map.of("role", m.get("role"), "content", m.get("content")));
                }
                msgs.add(Map.of("role", "user", "content", message));

                Map<String, Object> ue = new HashMap<>();
                ue.put("role", "user"); ue.put("content", message);
                ue.put("time", LocalDateTime.now().toString());
                hist.add(ue);

                Map<String, Object> body = new HashMap<>();
                body.put("model", model); body.put("messages", msgs);
                body.put("stream", true); body.put("max_tokens", 2048);

                conn = (HttpURLConnection) URI.create(baseUrl + "/chat/completions").toURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "text/event-stream");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(120000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(objectMapper.writeValueAsBytes(body));
                }

                StringBuilder full = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) break;
                            try {
                                Map<String, Object> chunk = objectMapper.readValue(data, Map.class);
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) chunk.get("choices");
                                if (choices != null && !choices.isEmpty()) {
                                    Map<String, Object> delta = (Map<String, Object>) choices.get(0).get("delta");
                                    if (delta != null && delta.get("content") != null) {
                                        String token = (String) delta.get("content");
                                        full.append(token);
                                        onToken.accept(token);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
                Map<String, Object> ae = new HashMap<>();
                ae.put("role", "assistant"); ae.put("content", full.toString());
                ae.put("time", LocalDateTime.now().toString());
                hist.add(ae);
                onComplete.run();
            } catch (Exception e) {
                log.error("AI chat error", e);
                onToken.accept("AI 服务异常: " + e.getMessage());
                onComplete.run();
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    @Override
    public Map<String, String> chatSync(String message, String conversationId, List<Long> fileIds) {
        AiApiConfig config = getBestConfig();
        String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();
        Map<String, String> result = new HashMap<>();
        result.put("conversationId", convId);

        if (config == null) {
            result.put("reply", "错误：未配置 AI 服务，请先在系统设置或 AI 配置中添加 API Key。");
            return result;
        }

        try {
            String apiKey = EncryptUtil.decrypt(config.getApiKey());
            String baseUrl = getBaseUrl(config);
            String model = config.getModelName() != null ? config.getModelName() : "gpt-3.5-turbo";

            List<Map<String, Object>> msgs = new ArrayList<>();
            msgs.add(Map.of("role", "system", "content", buildSystemPrompt(fileIds)));
            List<Map<String, Object>> hist = conversationStore.computeIfAbsent(convId, k -> new ArrayList<>());
            int s = Math.max(0, hist.size() - 10);
            for (int i = s; i < hist.size(); i++) {
                msgs.add(Map.of("role", hist.get(i).get("role"), "content", hist.get(i).get("content")));
            }
            msgs.add(Map.of("role", "user", "content", message));

            Map<String, Object> ue = new HashMap<>();
            ue.put("role", "user"); ue.put("content", message);
            ue.put("time", LocalDateTime.now().toString());
            hist.add(ue);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model); body.put("messages", msgs);
            body.put("stream", false); body.put("max_tokens", 2048);

            String respJson = httpPost(baseUrl + "/chat/completions", apiKey, body);
            Map<String, Object> resp = objectMapper.readValue(respJson, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            String reply = "";
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg != null) reply = (String) msg.get("content");
            }

            Map<String, Object> ae = new HashMap<>();
            ae.put("role", "assistant"); ae.put("content", reply);
            ae.put("time", LocalDateTime.now().toString());
            hist.add(ae);

            result.put("reply", reply != null ? reply : "AI 未返回有效响应");
        } catch (Exception e) {
            log.error("AI chat error", e);
            result.put("reply", "AI 服务异常: " + e.getMessage());
        }
        return result;
    }

    // ==================== Summary ====================

    @Override
    public String summarizeFile(Long fileId) {
        FileInfo file = fileInfoMapper.selectById(fileId);
        if (file == null) throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        AiApiConfig config = getBestConfig();
        if (config == null) return "请先配置 AI 服务";

        String text = extractFileText(file);
        if (text == null || text.isBlank()) return "无法提取该文件内容";
        String truncated = text.length() > 8000 ? text.substring(0, 8000) + "..." : text;

        try {
            String apiKey = EncryptUtil.decrypt(config.getApiKey());
            String baseUrl = getBaseUrl(config);
            Map<String, Object> body = Map.of("model", config.getModelName() != null ? config.getModelName() : "gpt-3.5-turbo",
                    "messages", List.of(
                            Map.of("role", "system", "content", "请用中文简要总结文件内容（200字以内）。"),
                            Map.of("role", "user", "content", truncated)),
                    "max_tokens", 500);
            String respJson = httpPost(baseUrl + "/chat/completions", apiKey, body);
            Map<String, Object> resp = objectMapper.readValue(respJson, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                if (msg != null) return (String) msg.get("content");
            }
            return respJson;
        } catch (Exception e) {
            return "AI 摘要失败: " + e.getMessage();
        }
    }

    // ==================== Conversations ====================

    @Override
    public List<Map<String, Object>> getConversations() {
        List<Map<String, Object>> list = new ArrayList<>();
        conversationStore.forEach((id, msgs) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", id);
            item.put("messageCount", msgs.size());
            item.put("lastMessage", msgs.isEmpty() ? "" : msgs.get(msgs.size() - 1).get("content"));
            item.put("updatedAt", msgs.isEmpty() ? "" : msgs.get(msgs.size() - 1).get("time"));
            list.add(item);
        });
        list.sort((a, b) -> String.valueOf(b.get("updatedAt")).compareTo(String.valueOf(a.get("updatedAt"))));
        return list;
    }

    @Override
    public Map<String, Object> getConversation(String id) {
        return Map.of("id", id, "messages", conversationStore.getOrDefault(id, Collections.emptyList()));
    }

    @Override
    public void deleteConversation(String id) { conversationStore.remove(id); }

    // ==================== Helpers ====================

    private AiApiConfig getBestConfig() {
        Long userId = JwtContextHolder.getUserId();
        AiApiConfig uc = aiApiConfigMapper.selectOne(
                new LambdaQueryWrapper<AiApiConfig>().eq(AiApiConfig::getUserId, userId)
                        .eq(AiApiConfig::getIsActive, 1).orderByDesc(AiApiConfig::getTested));
        if (uc != null) return uc;
        return aiApiConfigMapper.selectOne(
                new LambdaQueryWrapper<AiApiConfig>().eq(AiApiConfig::getIsSystem, 1)
                        .eq(AiApiConfig::getIsActive, 1).orderByDesc(AiApiConfig::getTested));
    }

    private String getBaseUrl(AiApiConfig config) {
        String url = config.getApiBaseUrl();
        if (url == null || url.isBlank()) {
            return switch (config.getApiType().toUpperCase()) {
                case "QWEN" -> "https://dashscope.aliyuncs.com/compatible-mode/v1";
                case "GLM" -> "https://open.bigmodel.cn/api/paas/v4";
                case "CLAUDE" -> "https://api.anthropic.com/v1";
                default -> "https://api.openai.com/v1";
            };
        }
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        return url;
    }

    private String httpPost(String url, String apiKey, Map<String, Object> body) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(60000);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(objectMapper.writeValueAsBytes(body));
        }
        return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String buildSystemPrompt(List<Long> fileIds) {
        StringBuilder sb = new StringBuilder("你是 TeamFiles 文件管理 AI 助手。帮助用户管理文件、搜索文件内容、回答相关问题。");
        if (fileIds != null && !fileIds.isEmpty()) {
            sb.append("\n\n用户引用了以下文件：\n");
            for (Long fid : fileIds) {
                FileInfo file = fileInfoMapper.selectById(fid);
                if (file == null) continue;
                sb.append("\n--- ").append(file.getOriginalName()).append(" ---\n");
                String text = extractFileText(file);
                if (text != null && !text.isBlank()) {
                    sb.append(text.length() > 3000 ? text.substring(0, 3000) + "..." : text);
                }
                sb.append("\n");
            }
            sb.append("\n请基于以上文件内容回答用户问题。使用中文回复。");
        } else {
            sb.append("使用中文回复。");
        }
        return sb.toString();
    }

    private String extractFileText(FileInfo file) {
        try {
            Path p = Paths.get(basePath, file.getFilePath());
            if (!Files.exists(p)) return null;
            String ext = file.getFileExtension() != null ? file.getFileExtension().toLowerCase() : "";
            if ("txt".equals(ext) || "md".equals(ext) || "csv".equals(ext)) return Files.readString(p);
            return "文件名: " + file.getOriginalName() + "\n类型: " + file.getFileType() +
                    "\n大小: " + file.getFileSize() + " bytes\n上传时间: " + file.getCreatedAt();
        } catch (IOException e) { return null; }
    }
}
