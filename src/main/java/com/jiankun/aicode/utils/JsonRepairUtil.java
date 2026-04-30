package com.jiankun.aicode.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * JSON 修复工具类
 * <p>
 * 用于修复 LLM（大语言模型）生成的格式错误的 JSON 字符串。
 * 在 AI 工具调用场景中，模型偶尔会生成缺少逗号、多余逗号等语法错误的 JSON，
 * 导致 Jackson 解析失败。本工具采用逐级修复策略，尽可能将畸形 JSON 修复为合法 JSON。
 * <p>
 * 修复策略（按优先级依次尝试）：
 * <ol>
 *   <li>修复键值对之间缺少的逗号 —— 最常见问题，如 {@code "a":"1" "b":"2"} → {@code "a":"1","b":"2"}</li>
 *   <li>修复尾部多余逗号 —— 如 {@code {"a":"1",} } → {@code {"a":"1"} }</li>
 *   <li>提取最外层 JSON 对象并重新修复 —— 处理 JSON 外部包裹了多余文本的情况</li>
 * </ol>
 * 每级修复后都会用 Jackson 验证，通过则立即返回，避免过度修复。
 */
@Slf4j
public class JsonRepairUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 修复格式错误的 JSON 字符串
     * <p>
     * 按优先级依次尝试三种修复策略，每种策略修复后立即验证，
     * 一旦验证通过就返回结果，避免不必要的后续修复操作。
     *
     * @param malformedJson 可能格式错误的 JSON 字符串
     * @return 修复后的合法 JSON 字符串；如果所有修复策略均失败则返回 null，
     *         如果输入为 null 或空白则返回 "{}"
     */
    public static String repairJson(String malformedJson) {
        if (malformedJson == null || malformedJson.isBlank()) {
            return "{}";
        }

        String json = malformedJson.trim();

        // 第一级修复：补全键值对之间缺少的逗号（LLM 最常见的问题）
        String repaired = fixMissingCommas(json);
        if (isValidJson(repaired)) {
            log.info("Successfully repaired JSON by fixing missing commas");
            return repaired;
        }

        // 第二级修复：移除尾部多余逗号
        repaired = fixTrailingCommas(repaired);
        if (isValidJson(repaired)) {
            log.info("Successfully repaired JSON by fixing trailing commas");
            return repaired;
        }

        // 第三级修复：提取最外层 {…} 并重新应用前两级修复
        // 处理 JSON 外部被多余文本包裹的情况，如：some text {"key":"value"} more text
        String extracted = extractAndRepairJsonObject(malformedJson);
        if (extracted != null && isValidJson(extracted)) {
            log.info("Successfully repaired JSON by extracting and repairing JSON object");
            return extracted;
        }

        // 所有修复策略均失败
        log.warn("Failed to repair JSON: {}", malformedJson);
        return null;
    }

    /**
     * 修复 JSON 中键值对之间缺少的逗号
     * <p>
     * 通过状态机逐字符扫描，跟踪当前是否处于字符串内部以及转义状态，
     * 在字符串值结束（闭合引号）后，如果下一个非空白字符是新的键（{@code "}）
     * 或嵌套对象（{@code {}）或嵌套数组（{@code [}），则自动插入逗号。
     * <p>
     * 示例：
     * <pre>
     *   输入: {"relativeFilePath": "src/App.vue" "content": "hello"}
     *   输出: {"relativeFilePath": "src/App.vue","content": "hello"}
     *          注意 "vue" 和 "content" 之间被插入了逗号 ↑
     * </pre>
     *
     * @param json 原始 JSON 字符串
     * @return 补全逗号后的 JSON 字符串
     */
    static String fixMissingCommas(String json) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;   // 当前是否在双引号字符串内部
        boolean escaped = false;    // 当前字符是否被反斜杠转义

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            // 处理转义字符的下一个字符：如果上一个字符是 \，则当前字符作为字面量输出
            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }

            // 在字符串内部遇到反斜杠，标记下一个字符为转义字符
            if (c == '\\' && inString) {
                result.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                if (inString) {
                    // 遇到闭合引号 —— 字符串值结束
                    result.append(c);
                    inString = false;

                    // 向前扫描跳过空白字符，检查是否需要补逗号
                    int j = i + 1;
                    while (j < json.length() && Character.isWhitespace(json.charAt(j))) {
                        j++;
                    }
                    // 如果下一个非空白字符是 "、{ 或 [，说明是新的键或嵌套结构，
                    // 需要在当前字符串值和下一个元素之间插入逗号
                    if (j < json.length() && (json.charAt(j) == '"' || json.charAt(j) == '{' || json.charAt(j) == '[')) {
                        result.append(',');
                    }
                } else {
                    // 遇到开放引号 —— 字符串开始
                    result.append(c);
                    inString = true;
                }
                continue;
            }

            // 非引号、非转义字符，直接输出
            result.append(c);
        }

        return result.toString();
    }

    /**
     * 修复 JSON 中尾部多余的逗号
     * <p>
     * 移除 } 或 ] 前面多余的逗号，这是 LLM 生成 JSON 时的另一种常见错误。
     * <p>
     * 示例：
     * <pre>
     *   输入: {"key": "value",}  →  输出: {"key": "value"}
     *   输入: [1, 2, 3,]        →  输出: [1, 2, 3]
     * </pre>
     *
     * @param json 原始 JSON 字符串
     * @return 移除尾部逗号后的 JSON 字符串
     */
    static String fixTrailingCommas(String json) {
        return json.replaceAll(",\\s*}", "}")
                .replaceAll(",\\s*]", "]");
    }

    /**
     * 从可能包含多余文本的字符串中提取 JSON 对象并修复
     * <p>
     * 定位最外层的 {@code {} 和 {@code }}，提取其中的内容，
     * 然后依次应用缺少逗号修复和尾部逗号修复。
     * <p>
     * 适用场景：模型在 JSON 前后添加了额外的说明文字，如：
     * <pre>
     *   输入: Here is the result: {"key": "value"} End of result
     *   提取: {"key": "value"}
     * </pre>
     *
     * @param json 可能包含多余文本的字符串
     * @return 提取并修复后的 JSON 字符串；如果找不到有效的 JSON 对象则返回 null
     */
    static String extractAndRepairJsonObject(String json) {
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String extracted = json.substring(start, end + 1);
            String repaired = fixMissingCommas(extracted);
            repaired = fixTrailingCommas(repaired);
            return repaired;
        }
        return null;
    }

    /**
     * 验证 JSON 字符串是否合法
     * <p>
     * 使用 Jackson 的 ObjectMapper 尝试将 JSON 解析为 Map，
     * 如果解析成功则说明 JSON 格式正确。
     *
     * @param json 待验证的 JSON 字符串
     * @return true 表示 JSON 合法，false 表示不合法
     */
    private static boolean isValidJson(String json) {
        try {
            OBJECT_MAPPER.readValue(json, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
