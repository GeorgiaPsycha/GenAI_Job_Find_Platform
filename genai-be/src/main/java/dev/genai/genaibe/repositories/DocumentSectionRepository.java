package dev.genai.genaibe.repositories;

import dev.genai.genaibe.models.entities.DocumentSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface DocumentSectionRepository extends JpaRepository<DocumentSection, UUID> {

    /* Semantic Serach in the DB using the distace of teh vectors to find the best
    infomation based on the question of the User
    @param questionEmbedding : the vector of the user question
     */
    @Query(nativeQuery = true
    , value = """
        SELECT ds.*
        FROM document_section ds inner join public.document d on d.id = ds.document_id
        WHERE d.account_id = :accountId
        ORDER BY CAST(ds.embedding AS vector) <-> cast(:questionEmbedding as vector)
        LIMIT :topK;
        
        """)
    List<DocumentSection> vectorSearch(String questionEmbedding, UUID accountId, int topK);
}
