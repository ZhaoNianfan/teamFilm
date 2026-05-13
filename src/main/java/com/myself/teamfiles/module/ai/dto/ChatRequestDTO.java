package com.myself.teamfiles.module.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequestDTO {
    private String message;
    private String conversationId;
    private List<Long> fileIds; // Referenced files for RAG context
}
