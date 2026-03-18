package com.jiankun.aicode.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jiankun.aicode.exception.ErrorCode;
import com.jiankun.aicode.exception.ThrowUtils;
import com.jiankun.aicode.model.enums.ChatHistoryMessageTypeEnum;
import com.jiankun.aicode.model.message.ToolExecutedMessage;
import com.jiankun.aicode.model.message.ToolRequestMessage;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.jiankun.aicode.model.entity.ChatHistoryOriginal;
import com.jiankun.aicode.mapper.ChatHistoryOriginalMapper;
import com.jiankun.aicode.service.ChatHistoryOriginalService;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 对话记忆 服务层实现。
 *
 * @author lijiankun
 * @since 2026/3/18
 */
@Slf4j
@Service
public class ChatHistoryOriginalServiceImpl extends ServiceImpl<ChatHistoryOriginalMapper, ChatHistoryOriginal> implements ChatHistoryOriginalService {

    /**
     * 添加对话记忆
     *
     * @param appId       应用id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      用户id
     * @return 添加结果
     */
    @Override
    public boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId) {
        // 参数校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "消息内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(messageType), ErrorCode.PARAMS_ERROR, "消息类型不能为空");
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR, "用户 ID不能为空");
        // 验证消息类型是否有效
        ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
        ThrowUtils.throwIf(messageTypeEnum == null, ErrorCode.SYSTEM_ERROR, "不支持的消息类型: " + messageType);
        // 对话消息入库
        ChatHistoryOriginal chatHistoryOriginal = ChatHistoryOriginal.builder()
                .appId(appId)
                .message(message)
                .messageType(messageType)
                .userId(userId)
                .build();
        return this.save(chatHistoryOriginal);
    }

    /**
     * 批量添加对话记忆
     *
     * @param chatHistoryOriginalList 对话记忆列表
     * @return 添加结果
     */
    @Override
    public boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList) {
        // 参数校验
        ThrowUtils.throwIf(chatHistoryOriginalList == null || chatHistoryOriginalList.isEmpty(),
                ErrorCode.PARAMS_ERROR, "消息列表不能为空");

        // 验证消息类型是否有效，无效类型的对话记录不进行入库
        List<ChatHistoryOriginal> validMessages = chatHistoryOriginalList.stream()
                .filter(chatHistory -> {
                    ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(chatHistory.getMessageType());
                    if (messageTypeEnum == null) {
                        log.error("不支持的消息类型: {}", chatHistory.getMessageType());
                        return false; // 过滤掉无效消息
                    }
                    return true; // 保留有效消息
                })
                .toList();

        // 如果没有有效消息，直接返回
        if (validMessages.isEmpty()) {
            return false;
        }

        // 批量入库
        return this.saveBatch(validMessages);
    }

    /**
     * 根据 appId 关联删除对话记忆记录
     *
     * @param appId 应用id
     * @return 删除结果
     */
    @Override
    public boolean deleteByAppId(Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID不能为空");
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("appId", appId);
        return this.remove(queryWrapper);
    }

    /**
     * 将 APP 的对话记忆加载到缓存中
     *
     * @param appId      应用ID
     * @param chatMemory 对话记忆
     * @param maxCount   最大加载条数
     * @return 加载的条数
     */
    @Override
    public int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount) {
        try {
            // 1. 查询记忆记录，考虑边缘记录类型
            List<ChatHistoryOriginal> originalHistoryList = queryHistoryWithEdgeCheck(appId, maxCount);
            if (CollUtil.isEmpty(originalHistoryList)) {
                return 0;
            }
            // 2. 反转列表，确保时间正序(老的在前，新的在后)
            originalHistoryList = originalHistoryList.reversed();
            // 3. 先清理当前 app 的记忆缓存，防止重复加载
            chatMemory.clear();
            // 4. 遍历原始记忆记录，根据类型将消息添加到记忆中
            int loadedCount = loadMessagesToMemory(originalHistoryList, chatMemory);
            log.info("成功为 appId: {} 加载 {} 条记忆对话", appId, loadedCount);
            return loadedCount;
        } catch (Exception e) {
            log.error("加载记忆对话失败，appId: {}，error: {}", appId, e.getMessage(), e);
            // 加载失败不影响系统运行，只是没有记忆上下文
            return 0;
        }
    }

    /**
     * 查询记忆记录，考虑边缘记录类型
     *
     * @param appId    应用ID
     * @param maxCount 最大记录数
     * @return 记忆记录列表
     */
    private List<ChatHistoryOriginal> queryHistoryWithEdgeCheck(Long appId, int maxCount) {
        // 1. 查询 maxCount + 1 条记录（跳过最新的一条 user message）
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(ChatHistoryOriginal::getAppId, appId)
                .orderBy(ChatHistoryOriginal::getId, false)
                .limit(1, maxCount + 1);

        List<ChatHistoryOriginal> historyList = this.list(queryWrapper);
        if (CollUtil.isEmpty(historyList)) {
            return Collections.emptyList();
        }
        // 2. 如果查出来的数据量连 maxCount 都没达到，说明记忆记录全查出来了
        if (historyList.size() <= maxCount) {
            // 防止数据库本身存在脏数据（最后一条恰好是孤立的 RESULT）
            ChatHistoryOriginal oldestRecord = historyList.get(historyList.size() - 1);
            if (ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT.getValue().equals(oldestRecord.getMessageType())) {
                log.warn("记忆记录底部发现孤立的 TOOL_EXECUTION_RESULT，为防大模型报错，已将其丢弃");
                historyList.remove(historyList.size() - 1);
            }
            return historyList;
        }
        // 3. 如果查出了 maxCount + 1 条记录，说明记忆记录很长，我们需要处理边缘
        // 前 maxCount 条是我们理想中需要的。第 maxCount 条（索引为 maxCount - 1）就是“边缘记录”
        ChatHistoryOriginal edgeRecord = historyList.get(maxCount - 1);
        if (ChatHistoryMessageTypeEnum.TOOL_EXECUTION_RESULT.getValue().equals(edgeRecord.getMessageType())) {
            // 边缘是 RESULT，我们必须保留多查出来的那第 maxCount + 1 条（它应该是对应的 REQUEST）
            ChatHistoryOriginal extraRecord = historyList.get(maxCount);
            // 双重保险：确保多出来的一条真的是 REQUEST
            if (ChatHistoryMessageTypeEnum.TOOL_EXECUTION_REQUEST.getValue().equals(extraRecord.getMessageType())) {
                return historyList; // 返回全部 maxCount + 1 条
            } else {
                // 脏数据异常：RESULT 后面跟着的竟然不是 REQUEST。为了安全，丢弃孤立的 RESULT
                log.warn("数据异常：边缘 RESULT 找不到对应的 REQUEST");
                historyList.remove(maxCount);     // 移除多余的错误记录
                historyList.remove(maxCount - 1); // 移除孤立的边缘 RESULT
                return historyList;
            }
        } else {
            // 边缘不是 RESULT，那么我们根本不需要多查出来的那第 maxCount + 1 条记录，直接丢弃
            historyList.remove(maxCount);
            return historyList;
        }
    }

    /**
     * 将记忆记录加载到内存中
     *
     * @param originalHistoryList 记忆记录列表
     * @param chatMemory          对话记忆
     * @return 加载的记录条数
     */
    private int loadMessagesToMemory(List<ChatHistoryOriginal> originalHistoryList, MessageWindowChatMemory chatMemory) {
        int loadedCount = 0;
        // 遍历原始记忆记录，根据类型将消息添加到记忆中
        for (ChatHistoryOriginal history : originalHistoryList) {
            // 这里需要根据消息类型进行转换，支持 AI, user, toolExecutionRequest, toolExecutionResult 4种类型
            String messageType = history.getMessageType();
            ChatHistoryMessageTypeEnum messageTypeEnum = ChatHistoryMessageTypeEnum.getEnumByValue(messageType);
            switch (messageTypeEnum) {
                case USER -> {
                    chatMemory.add(UserMessage.from(history.getMessage()));
                    loadedCount++;
                }
                case AI -> {
                    chatMemory.add(AiMessage.from(history.getMessage()));
                    loadedCount++;
                }
                case TOOL_EXECUTION_REQUEST -> {
                    ToolRequestMessage toolRequestMessage = JSONUtil.toBean(history.getMessage(), ToolRequestMessage.class);
                    ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                            .id(toolRequestMessage.getId())
                            .name(toolRequestMessage.getName())
                            .arguments(toolRequestMessage.getArguments())
                            .build();
                    // 有些工具调用请求带有文本，有些没有
                    if (toolRequestMessage.getText().isEmpty()) {
                        chatMemory.add(AiMessage.from(List.of(toolExecutionRequest)));
                    } else {
                        chatMemory.add(AiMessage.from(toolRequestMessage.getText(), List.of(toolExecutionRequest)));
                    }
                    loadedCount++;
                }
                case TOOL_EXECUTION_RESULT -> {
                    ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(history.getMessage(), ToolExecutedMessage.class);
                    String id = toolExecutedMessage.getId();
                    String toolName = toolExecutedMessage.getName();
                    String toolExecutionResult = toolExecutedMessage.getResult();
                    chatMemory.add(ToolExecutionResultMessage.from(id, toolName, toolExecutionResult));
                    loadedCount++;
                }
                case null -> log.error("未知消息类型: {}", messageType);
            }
        }
        return loadedCount;
    }
}
