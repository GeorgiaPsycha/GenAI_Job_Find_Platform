package dev.genai.genaibe.models.dtos.completions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmbeddingResponse {

    private String object;
    private String model;
    private List<EmbeddingData> data;
    private Usage usage;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class EmbeddingData {

        private String object;
        private int index;
        private List<Double> embedding;
    }


}
