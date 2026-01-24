package dev.genai.genaibe.services;

import dev.genai.genaibe.models.dtos.completions.voyageai.RerankRequest;
import dev.genai.genaibe.models.dtos.completions.voyageai.RerankResponse;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.DocumentSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReRankingApiService {

    Logger logger = LoggerFactory.getLogger(ReRankingApiService.class);
    private String apiKey;

    public ReRankingApiService(@Value("${llms.voyage.key}") String apiKey) {
        this.apiKey = apiKey;
    }

    public List<DocumentSection> rerankDocuments(Agent agent, String query, List<DocumentSection> chunks) {
        String url = "https://api.voyageai.com/v1/rerank";

        return rerankDocuments(url, agent.getRerankingModel(), query, chunks);

    }

    /**
     *  Rerank documents using the specified model and query.
     *
     * @param url
     * @param model the model to use for reranking
     * @param query the user's question
     * @param chunks the list of document sections to rerank
     * @return the reranked list of document sections
     */
    public List<DocumentSection> rerankDocuments(String url, String model, String query, List<DocumentSection> chunks) {
        RestTemplate restTemplate = new RestTemplate();

        // 2. Build headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        RerankRequest rerankRequest = RerankRequest.builder()
                .model(model)
                .query(query)
                .documents(chunks.stream().map(DocumentSection::getContent).toList())
                .build();

        logger.debug("Request body: {}", rerankRequest);
        HttpEntity<RerankRequest> request = new HttpEntity<>(rerankRequest, headers);
        ResponseEntity<RerankResponse> response = restTemplate.postForEntity(
                url,
                request,
                RerankResponse.class);

        logger.debug("Response body: {}", response);

        RerankResponse rerankResponse = response.getBody();

        List<DocumentSection> sortedChunks = sortDocuments(chunks, rerankResponse.getData());

        return sortedChunks;

    }


    public List<DocumentSection> sortDocuments(
            List<DocumentSection> originalDocuments,
            List<RerankResponse.RerankResult> rerankResults
    ) {

        List<DocumentSection> sorted = new ArrayList<>(rerankResults.size());

        for (RerankResponse.RerankResult result : rerankResults) {
            int originalIndex = result.getIndex();

            if (originalIndex < 0 || originalIndex >= originalDocuments.size()) {
                throw new IllegalArgumentException(
                        "Invalid document index returned by rerank API: " + originalIndex
                );
            }

            sorted.add(originalDocuments.get(originalIndex));
        }

        return sorted;
    }

}
