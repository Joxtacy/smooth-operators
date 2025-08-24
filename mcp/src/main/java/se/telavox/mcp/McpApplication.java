package se.telavox.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class McpApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpApplication.class, args);
    }

    @Bean
    public ToolCallbackProvider weatherTools(WeatherService weatherService) {
        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    }

    public record TextInput(String input) {
    }

    @Bean
    public ToolCallback toUpperCase() {
        return FunctionToolCallback.builder("toUpperCase", (TextInput input) -> input.input().toUpperCase())
                .inputType(TextInput.class)
                .description("Put the text to upper case")
                .build();
    }

    @Bean
    public List<McpServerFeatures.AsyncPromptSpecification> myPrompts() {
        var prompt = new McpSchema.Prompt("greeting", "A friendly greeting prompt",
                List.of(new McpSchema.PromptArgument("name", "The name to greet", true)));

        var promptSpecification = new McpServerFeatures.AsyncPromptSpecification(prompt, (exchange, getPromptRequest) -> {
            var nameArgument = (String) getPromptRequest.arguments().get("name");
            if (nameArgument == null) {
                nameArgument = "friend";
            }

            var userMessage = new McpSchema.PromptMessage(
                    McpSchema.Role.USER,
                    new McpSchema.TextContent("Hello " + nameArgument + "! How can I assist you today?")
            );
            return Mono.just(new McpSchema.GetPromptResult("A personalized greeting message", List.of(userMessage)));
        });

        return List.of(promptSpecification);
    }

    @Bean
    public List<McpServerFeatures.AsyncResourceSpecification> myResources() {
        var systemInfoResource = new McpSchema.Resource("github://repos/{owner}/{repo}", "GitHub Repository",
                "Repository Information for a given GitHub repository",
                "application/json",
                new McpSchema.Annotations(List.of(McpSchema.Role.USER), 0.1)
        );
        var resourceSpecification = new McpServerFeatures.AsyncResourceSpecification(systemInfoResource,
                (exchange, request) -> {
                    try {
                        var systemInfo = Map.of("hi", "hello");
                        String jsonContent = new ObjectMapper().writeValueAsString(systemInfo);
                        return Mono.just(new McpSchema.ReadResourceResult(
                                List.of(new McpSchema.TextResourceContents(request.uri(), "application/json",
                                        jsonContent))));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to generate system info", e);
                    }
                });

        return List.of(resourceSpecification);
    }

}
