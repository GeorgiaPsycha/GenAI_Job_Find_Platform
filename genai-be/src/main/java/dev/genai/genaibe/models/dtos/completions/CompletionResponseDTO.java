package dev.genai.genaibe.models.dtos.completions;

import dev.genai.genaibe.models.entities.ChatMessage;
import dev.genai.genaibe.models.entities.DocumentSection;
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
