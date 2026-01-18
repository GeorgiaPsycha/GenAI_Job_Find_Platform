package dev.ctrlspace.genai2506.genaibe.models.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "title", nullable = false, length = Integer.MAX_VALUE)
    private String title;

    @Column(name = "summary", length = Integer.MAX_VALUE)
    private String summary;

    @Column(name = "body", nullable = false, length = Integer.MAX_VALUE)
    private String body;

    @Column(name = "company", length = Integer.MAX_VALUE)
    private String company;

    @Column(name = "location", length = Integer.MAX_VALUE)
    private String location;

    @Column(name = "seniority", length = Integer.MAX_VALUE)
    private String seniority;

    @Column(name = "tags", length = Integer.MAX_VALUE)
    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ColumnDefault("'active'")
    @Column(name = "status", nullable = false, length = Integer.MAX_VALUE)
    private String status;

}