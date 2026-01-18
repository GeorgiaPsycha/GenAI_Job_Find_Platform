package dev.ctrlspace.genai2506.genaibe.models.dtos.completions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContentPart {


    private String type;
    private String text;

}
