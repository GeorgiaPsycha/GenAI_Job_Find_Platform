package dev.genai.genaibe.tools;

import dev.genai.genaibe.models.dtos.completions.MessageDTO;
import dev.genai.genaibe.models.entities.Agent;
import dev.genai.genaibe.models.entities.ChatMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.logging.Logger;

@Component
public class SqlRunnerTool implements Tool {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    Logger logger = Logger.getLogger(SqlRunnerTool.class.getName());

    public SqlRunnerTool(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getName() {
        return "run_select_query";
    }

    @Override
    public String getDescription() {
        return "Executes a SQL SELECT query against the database to retrieve information about users, jobs, or applications. Use this to inspect the database schema or data.";
    }

    @Override
    public String getParameters() {
        // Tell the LLM that its waiting for a query
        return """
            {
                "type": "object",
                "properties": {
                    "query": {
                        "type": "string",
                        "description": "The SQL SELECT query to execute."
                    }
                },
                "required": ["query"]
            }
            """;
    }

    @Override
    public MessageDTO execute(MessageDTO.ToolCall toolCall, Agent agent, ChatMessage message) throws Exception {

        JsonNode arguments = objectMapper.readTree(toolCall.getFunction().getArguments());
        String query = arguments.get("query").asText();

        logger.info("Executing query: " + query);
        // run the query in the DB
        String result = jdbcTemplate.query(query, rs -> {
            StringBuilder sb = new StringBuilder();
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // header row with column names
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) sb.append(",");
                sb.append(meta.getColumnLabel(i));
            }
            sb.append("\n");

            // convert the text into CSV format
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) sb.append(",");
                    Object value = rs.getObject(i);
                    sb.append(value != null ? value.toString() : "");
                }
                sb.append("\n");
            }

            return sb.toString();
        });

        return MessageDTO.builder()
                .role("tool")
                .content("SQL Query Result:\n" + result)
                .build();
    }
}
