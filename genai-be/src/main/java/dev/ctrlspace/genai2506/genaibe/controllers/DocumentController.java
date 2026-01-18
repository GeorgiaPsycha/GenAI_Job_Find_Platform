package dev.ctrlspace.genai2506.genaibe.controllers;


import dev.ctrlspace.genai2506.genaibe.models.dtos.DocumentCriteria;
import dev.ctrlspace.genai2506.genaibe.models.entities.Document;
import dev.ctrlspace.genai2506.genaibe.services.DocumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DocumentController {


    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }


    @GetMapping("/documents")
    public List<Document> getAllDocuments(DocumentCriteria criteria) {
        return documentService.getAll(criteria);
    }

    @PostMapping("/documents")
    public Document createDocument(@RequestBody Document document) {

        return documentService.createDocument(document);
    }


}
