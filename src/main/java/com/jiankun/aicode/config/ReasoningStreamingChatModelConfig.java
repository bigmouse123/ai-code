package com.jiankun.aicode.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * 推理流式模型配置
 *
 * @author lijiankun
 * @since 2026/3/12
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
@Slf4j
public class ReasoningStreamingChatModelConfig {

    private static final String DEEPSEEK_V4_FLASH_MODEL = "deepseek-v4-flash";

    private String baseUrl;
    private String apiKey;

    private Integer maxTokens = 8192;
    private Boolean logRequests = true;
    private Boolean logResponses = true;

    // true: thinking mode, false: non-thinking mode
    private Boolean thinkingEnabled = false;

    // low / medium / high / max (effective when thinkingEnabled=true)
    private String reasoningEffort = "high";

    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        boolean enableThinking = Boolean.TRUE.equals(thinkingEnabled);

        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(DEEPSEEK_V4_FLASH_MODEL)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses);

        applyThinkingTransport(builder, enableThinking);

        boolean thinkingConfigured = tryApplyThinkingMode(builder, enableThinking);
        if (!thinkingConfigured) {
            throw new IllegalStateException("Current langchain4j/open-ai builder does not support thinking mode config. "
                    + "Please upgrade dependencies that support sendThinking(...) or customParameters(...).");
        }

        if (enableThinking && reasoningEffort != null && !reasoningEffort.isBlank()) {
            applyReasoningEffort(builder, reasoningEffort);
        }

        return builder.build();
    }

    private boolean tryApplyThinkingMode(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder,
                                         boolean enableThinking) {
        // DeepSeek thinking mode switch: customParameters({thinking:{type:enabled|disabled}})
        Map<String, Object> thinkingConfig = Map.of(
                "thinking", Map.of("type", enableThinking ? "enabled" : "disabled")
        );
        return invokeMethod(builder, "customParameters", new Class[]{Map.class}, new Object[]{thinkingConfig});
    }

    private void applyThinkingTransport(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder,
                                        boolean enableThinking) {
        // Parse reasoning_content from response into AiMessage.thinking()
        invokeMethod(builder, "returnThinking", new Class[]{Boolean.class}, new Object[]{enableThinking});

        // Send AiMessage.thinking() back as reasoning_content in follow-up tool-call turns.
        boolean applied = invokeMethod(builder, "sendThinking",
                new Class[]{Boolean.class, String.class}, new Object[]{enableThinking, "reasoning_content"});
        if (!applied) {
            invokeMethod(builder, "sendThinking", new Class[]{Boolean.class}, new Object[]{enableThinking});
        }
    }

    private void applyReasoningEffort(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder,
                                      String effort) {
        // Preferred path on newer versions: reasoningEffort(String)
        if (invokeMethod(builder, "reasoningEffort", new Class[]{String.class}, new Object[]{effort})) {
            return;
        }

        // Fallback path for older versions
        builder.defaultRequestParameters(OpenAiChatRequestParameters.builder()
                .reasoningEffort(effort)
                .build());
    }

    private boolean invokeMethod(Object target, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = target.getClass().getMethod(methodName, paramTypes);
            method.invoke(target, args);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
