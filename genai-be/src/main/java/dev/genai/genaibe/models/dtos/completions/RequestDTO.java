package dev.genai.genaibe.models.dtos.completions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.JsonNode;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestDTO {

    private String model;
    private List<MessageDTO> messages;

    @JsonProperty("reasoning_effort")
    private String reasoningEffort;

    @JsonProperty("max_completion_tokens")
    private Integer maxCompletionTokens;
    @JsonProperty("temperature")
    private Double temperature;


    @JsonProperty("tool_choice")
    private  String toolChoice;

    private List<JsonNode> tools;

}
