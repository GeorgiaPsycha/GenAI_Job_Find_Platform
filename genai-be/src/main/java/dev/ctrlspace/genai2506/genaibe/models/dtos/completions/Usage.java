package dev.ctrlspace.genai2506.genaibe.models.dtos.completions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usage {
    @JsonProperty("queue_time")
    private Double queueTime;

    @JsonProperty("prompt_tokens")
    private Integer promptTokens;

    @JsonProperty("prompt_time")
    private Double promptTime;

    @JsonProperty("completion_tokens")
    private Integer completionTokens;

    @JsonProperty("completion_time")
    private Double completionTime;

    @JsonProperty("total_tokens")
    private Integer totalTokens;

    @JsonProperty("total_time")
    private Double totalTime;
}