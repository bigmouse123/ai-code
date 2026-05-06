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
    private static final String DEEPSEEK_CHAT_COMPAT_MODEL = "deepseek-chat";
    private static final String DEEPSEEK_REASONER_COMPAT_MODEL = "deepseek-reasoner";

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
        OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(DEEPSEEK_V4_FLASH_MODEL)
                .maxTokens(maxTokens)
                .logRequests(logRequests)
                .logResponses(logResponses);

        // DeepSeek V4 switches thinking mode via extra_body.thinking.type.
        // If current LangChain4j cannot pass custom parameters, fallback to compat model names.
        boolean thinkingConfigured = tryApplyThinkingMode(builder, Boolean.TRUE.equals(thinkingEnabled));
        if (!thinkingConfigured) {
            String compatibilityModel = Boolean.TRUE.equals(thinkingEnabled)
                    ? DEEPSEEK_REASONER_COMPAT_MODEL
                    : DEEPSEEK_CHAT_COMPAT_MODEL;
            builder.modelName(compatibilityModel);
            log.warn("Current LangChain4j version does not support customParameters; fallback to compatibility model [{}].",
                    compatibilityModel);
        }

        if (Boolean.TRUE.equals(thinkingEnabled) && reasoningEffort != null && !reasoningEffort.isBlank()) {
            builder.defaultRequestParameters(OpenAiChatRequestParameters.builder()
                    .reasoningEffort(reasoningEffort)
                    .build());
        }

        return builder.build();
    }

    private boolean tryApplyThinkingMode(OpenAiStreamingChatModel.OpenAiStreamingChatModelBuilder builder,
                                         boolean enableThinking) {
        try {
            Method customParametersMethod = builder.getClass().getMethod("customParameters", Map.class);
            Map<String, Object> thinkingConfig = Map.of(
                    "thinking", Map.of("type", enableThinking ? "enabled" : "disabled")
            );
            customParametersMethod.invoke(builder, thinkingConfig);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
