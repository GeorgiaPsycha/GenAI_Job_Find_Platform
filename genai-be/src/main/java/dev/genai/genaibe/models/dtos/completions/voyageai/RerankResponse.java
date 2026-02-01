package dev.genai.genaibe.models.dtos.completions.voyageai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.genai.genaibe.models.dtos.completions.Usage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class RerankResponse {

    private String object;
    private List<RerankResult> data;
    private String model;
    private Usage usage;


    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RerankResult {

        @JsonProperty("relevance_score")
        private double relevanceScore;

        private int index;
    }
}
