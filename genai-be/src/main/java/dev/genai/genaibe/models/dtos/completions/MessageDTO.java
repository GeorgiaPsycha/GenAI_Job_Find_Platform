package dev.genai.genaibe.models.dtos.completions;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.genai.genaibe.models.entities.DocumentSection;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageDTO {

    private String role;
    private String content;
    @JsonProperty("tool_call_id")
    private String toolCallId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("tool_calls")
    private List<ToolCall> toolCalls;

    @JsonIgnore
    private List<DocumentSection> supportingDocuments = new ArrayList<>();


    public static class ToolCall {
        @Setter
        @Getter
        private String id;
        private String type;
        @Getter
        private FunctionCall function;

    }

    public static class FunctionCall {
        private String arguments;
        private String name;

        public String getArguments() {
            return arguments;
        }

        public void setArguments(String arguments) {
            this.arguments = arguments;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }


}
