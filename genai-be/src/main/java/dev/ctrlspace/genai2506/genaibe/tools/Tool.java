package dev.ctrlspace.genai2506.genaibe.tools;

import dev.ctrlspace.genai2506.genaibe.models.dtos.completions.MessageDTO;
import dev.ctrlspace.genai2506.genaibe.models.entities.Agent;
import dev.ctrlspace.genai2506.genaibe.models.entities.ChatMessage;

public interface Tool {

    String getName();
    MessageDTO execute(MessageDTO.ToolCall toolCall, Agent agent, ChatMessage message) throws Exception;
}
