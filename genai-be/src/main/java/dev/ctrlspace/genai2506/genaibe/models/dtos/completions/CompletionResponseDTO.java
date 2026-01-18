package dev.ctrlspace.genai2506.genaibe.models.dtos.completions;

import dev.ctrlspace.genai2506.genaibe.models.entities.ChatMessage;
import dev.ctrlspace.genai2506.genaibe.models.entities.DocumentSection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompletionResponseDTO {

    private ChatMessage message;
    private List<DocumentSection> supportingDocuments;
}
