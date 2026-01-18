package dev.ctrlspace.genai2506.genaibe.models.dtos.completions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResponse {
    private String id;
    private String object;
    private long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;
    @JsonProperty("service_tier")
    private String serviceTier;
}
