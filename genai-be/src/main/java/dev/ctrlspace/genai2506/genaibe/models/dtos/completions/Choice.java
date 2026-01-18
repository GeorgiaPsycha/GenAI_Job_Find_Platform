package dev.ctrlspace.genai2506.genaibe.models.dtos.completions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Choice {
    private int index;
    private MessageDTO message;
    private Object logprobs;

    @JsonProperty("finish_reason")
    private String finishReason;
}
