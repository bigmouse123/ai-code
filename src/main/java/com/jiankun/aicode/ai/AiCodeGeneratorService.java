package com.jiankun.aicode.ai;

import com.jiankun.aicode.model.HtmlCodeResult;
import com.jiankun.aicode.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;

/**
 * AI 代码生成服务
 *
 * @since 2026/2/22
 */
public interface AiCodeGeneratorService {
    /**
     * 生成 HTML 代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码
     *
     * @param userMessage 用户消息
     * @return 生成的代码结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

}

