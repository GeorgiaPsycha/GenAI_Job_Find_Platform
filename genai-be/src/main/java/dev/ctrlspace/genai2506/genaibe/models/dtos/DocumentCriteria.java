package dev.ctrlspace.genai2506.genaibe.models.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentCriteria {

    private String searchText;
    private String accountId;
}
