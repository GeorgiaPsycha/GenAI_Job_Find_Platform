package dev.genai.genaibe.services;

import dev.genai.genaibe.models.dtos.DocumentCriteria;
import dev.genai.genaibe.models.dtos.completions.EmbeddingResponse;
import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.Document;
import dev.genai.genaibe.models.entities.DocumentSection;
import dev.genai.genaibe.repositories.DocumentRepository;
import dev.genai.genaibe.repositories.DocumentSectionRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {



    private final CompletionsApiService completionsApiService;
    private final DocumentRepository documentRepository;
    private final DocumentSectionRepository documentSectionRepository;
    private final ReRankingApiService reRankingApiService;

    public DocumentService(CompletionsApiService completionsApiService, DocumentRepository documentRepository, DocumentSectionRepository documentSectionRepository, ReRankingApiService reRankingApiService) {
        this.completionsApiService = completionsApiService;
        this.documentRepository = documentRepository;
        this.documentSectionRepository = documentSectionRepository;
        this.reRankingApiService = reRankingApiService;
    }

    public Document createDocument(Document document) {
        document.setStatus("active");
        // create document
        // Save the Parent Document (Job Post)
        document = documentRepository.save(document);

        //Prepare Content for Indexing (Job Description + Metadata)
        String contentToIndex = String.format(
                "Title: %s\nCompany: %s\nLocation: %s\nSeniority: %s\n\nDescription:\n%s",
                document.getTitle(),
                document.getCompany() != null ? document.getCompany() : "",
                document.getLocation() != null ? document.getLocation() : "",
                document.getSeniority() != null ? document.getSeniority() : "",
                document.getBody()
        );

        // index document
        List<DocumentSection> documentSections = indexDocument(document, contentToIndex);

        return document;

    }

    public List<DocumentSection> indexDocument(Document document, String content) {

        List<String> sections = splitContentIntoSections(content, 500);

        Agent agent = new Agent();
        agent.setBehavior("""
                You are a DB Admin, this is a Postgres DB, users will ask you stuff, and you will run as many queries as needed in the DB to achive the goal. You dont know the schema, run queries to find it.
                """);
        agent.setLlmModel("llama3.1:latest");
        agent.setEmbeddingsModel("nomic-embed-text:latest");
        agent.setTemperature(1.0);
        agent.setMaxTokens(1000);

        List<DocumentSection> documentSections = new ArrayList<>(sections.size());

        int i = 0;

        for (String section : sections) {

            MessageDTO message = new MessageDTO();
            message.setRole("user");
            message.setContent(section);
            // call for  Embedding
            EmbeddingResponse embeddingResponse = completionsApiService.getEmbedding(agent, message);

            // Save Section with Vector
            DocumentSection documentSection = new DocumentSection();
            documentSection.setDocument(document);
            documentSection.setSectionIndex(i++);
            documentSection.setContent(section);
            //take the vector from response
            if (embeddingResponse != null && !embeddingResponse.getData().isEmpty()) {
                documentSection.setEmbedding(embeddingResponse.getData().get(0).getEmbedding());
            }
            documentSectionRepository.save(documentSection);
            documentSections.add(documentSection);
        }

        return documentSections;
    }


    public List<DocumentSection> vectorSearch(String question, UUID accountId, int topK) {


        Agent agent = getDefaultAgent();

        MessageDTO message = new MessageDTO();
        message.setRole("user");
        message.setContent(question);

        EmbeddingResponse embeddingResponse = completionsApiService.getEmbedding(agent, message);


        return documentSectionRepository.vectorSearch(embeddingResponse.getData().getFirst().getEmbedding().toString(), accountId, topK);
    }

    /*
     * Splits the content into sections; each section has at most {@code wordLimit} words.
     * @param content   the text to split
     * @param wordLimit maximum number of words per section (must be > 0)
     * @return a list of sections, each containing up to {@code wordLimit} words
     */
    private List<String> splitContentIntoSections(String content, int wordLimit) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }
        if (wordLimit <= 0) {
            throw new IllegalArgumentException("wordLimit must be greater than 0");
        }

        String[] words = content.trim().split("\\s+");
        List<String> sections = new ArrayList<>();

        StringBuilder currentSection = new StringBuilder();
        int wordCount = 0;

        for (String word : words) {
            if (wordCount == 0) {
                // first word in the section
                currentSection.append(word);
            } else if (wordCount < wordLimit) {
                currentSection.append(' ').append(word);
            } else {
                sections.add(currentSection.toString());
                currentSection.setLength(0);
                currentSection.append(word);
                wordCount = 0;
            }
            wordCount++;

            if (wordCount == wordLimit) {
                sections.add(currentSection.toString());
                currentSection.setLength(0);
                wordCount = 0;
            }
        }

        // Add any remaining words in the last section
        if (currentSection.length() > 0 && wordCount > 0) {
            sections.add(currentSection.toString());
        }

        return sections;
    }


    public List<Document> getAll(DocumentCriteria criteria) {
        if (criteria != null && criteria.getSearchText() != null && !criteria.getSearchText().trim().isEmpty()) {

            Agent agent = getDefaultAgent();

            List<DocumentSection> sections = this.semanticSearch(agent,
                    UUID.fromString(criteria.getAccountId()),
                    criteria.getSearchText(),
                    criteria.getSearchText(),
                    10);

            return sections.stream().map(s -> s.getDocument()).distinct().toList();
        }
        return documentRepository.findAll();
    }


    public List<DocumentSection> semanticSearch(Agent agent, UUID accountId, String originalMessage, String query, int resultsCount) {
        List<DocumentSection> relevantSections = this.vectorSearch(query, accountId, resultsCount*4);

        // re-rank sections using
        relevantSections = reRankingApiService.rerankDocuments(agent, originalMessage, relevantSections);

        relevantSections = relevantSections.subList(0, resultsCount);
        return relevantSections;
    }



    private static Agent getDefaultAgent() {
        Agent agent = new Agent();
        agent.setBehavior("""
            You are a DB Admin, this is a Postgres DB, users will ask you stuff, and you will run as many queries as needed in the DB to achive the goal. You dont know the schema, run queries to find it.
            """);
        agent.setLlmModel("llama3.2");
        agent.setEmbeddingsModel("nomic-embed-text");
        agent.setRerankingModel("rerank-2.5-lite");
        agent.setTemperature(1.0);
        agent.setMaxTokens(1000);
        return agent;
    }
}
