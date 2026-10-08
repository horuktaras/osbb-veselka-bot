package ua.horuktaras.osbb.bot.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import ua.horuktaras.osbb.bot.model.entity.Request;

import java.util.List;

@Service
public class AiDuplicateService {

    private static final Logger log = LoggerFactory.getLogger(AiDuplicateService.class);

    private final RestClient groqClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final boolean enabled;

    public AiDuplicateService(RestClient groqClient,
                               ObjectMapper objectMapper,
                               @Value("${groq.api-key:}") String apiKey,
                               @Value("${groq.model:llama-3.3-70b-versatile}") String model) {
        this.groqClient = groqClient;
        this.objectMapper = objectMapper;
        this.model = model;
        this.enabled = !apiKey.isBlank();
        if (!this.enabled) {
            log.warn("GROQ_API_KEY not set — AI duplicate check disabled");
        }
    }

    /**
     * Returns IDs of open requests that are similar to the new one.
     * Returns empty list if AI is disabled or check fails.
     */
    public List<Long> findSimilar(String newDescription, String newType, List<Request> openRequests) {
        if (!enabled || openRequests.isEmpty()) return List.of();

        String promptText = buildPrompt(newDescription, newType, openRequests);
        try {
            GroqRequest req = new GroqRequest(
                    model,
                    List.of(new GroqMessage("user", promptText)),
                    0.1,
                    100
            );

            GroqResponse response = groqClient.post()
                    .uri("/chat/completions")
                    .body(req)
                    .retrieve()
                    .body(GroqResponse.class);

            if (response == null || response.choices().isEmpty()) return List.of();

            String content = response.choices().get(0).message().content().trim();
            log.debug("Groq duplicate check response: {}", content);

            int start = content.indexOf('[');
            int end = content.lastIndexOf(']');
            if (start == -1 || end == -1) return List.of();

            return objectMapper.readValue(content.substring(start, end + 1), new TypeReference<>() {});

        } catch (ResourceAccessException e) {
            log.warn("AI duplicate check timed out or unreachable, skipping: {}", e.getMessage());
            return List.of();
        } catch (RestClientResponseException e) {
            log.warn("AI duplicate check returned HTTP {}, skipping: {}", e.getStatusCode(), e.getMessage());
            return List.of();
        } catch (Exception e) {
            log.warn("AI duplicate check failed, skipping: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildPrompt(String newDescription, String newType, List<Request> openRequests) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a duplicate detector for a Ukrainian housing association (OSBB) request system.\n\n");
        sb.append("New request:\n");
        sb.append("Type: ").append(newType).append("\n");
        sb.append("Description: ").append(newDescription).append("\n\n");
        sb.append("Existing open requests:\n");
        for (Request r : openRequests) {
            sb.append("#").append(r.getId())
              .append(" | ").append(r.getType().name())
              .append(" | ").append(r.getDescription()).append("\n");
        }
        sb.append("\nReturn ONLY a JSON array of IDs of requests that describe the same or very similar problem as the new request.\n");
        sb.append("Example: [12, 15]\n");
        sb.append("If none are similar, return: []\n");
        sb.append("Only return the JSON array, nothing else.");
        return sb.toString();
    }

    // ── Groq API DTOs ────────────────────────────────────────────────────────

    private record GroqMessage(String role, String content) {}

    private record GroqRequest(
            String model,
            List<GroqMessage> messages,
            double temperature,
            @JsonProperty("max_tokens") int maxTokens
    ) {}

    private record GroqChoiceMessage(String content) {}

    private record GroqChoice(GroqChoiceMessage message) {}

    private record GroqResponse(List<GroqChoice> choices) {}
}
