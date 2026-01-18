package dev.ctrlspace.genai2506.genaibe.repositories;

import dev.ctrlspace.genai2506.genaibe.models.entities.Document;
import dev.ctrlspace.genai2506.genaibe.models.entities.DocumentSection;
import dev.ctrlspace.genai2506.genaibe.models.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public interface DocumentSectionRepository extends JpaRepository<DocumentSection, UUID> {


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
