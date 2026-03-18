package com.jiankun.aicode.service;

import com.mybatisflex.core.service.IService;
import com.jiankun.aicode.model.entity.ChatHistoryOriginal;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.List;

/**
 * 对话记忆 服务层。
 *
 * @author lijiankun
 * @since 2026/3/18
 */
public interface ChatHistoryOriginalService extends IService<ChatHistoryOriginal> {
    /**
     * 添加对话记忆
     *
     * @param appId       应用id
     * @param message     消息
     * @param messageType 消息类型
     * @param userId      用户id
     * @return 添加结果
     */
    boolean addOriginalChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 批量添加对话记忆
     *
     * @param chatHistoryOriginalList 对话记忆列表
     * @return 添加结果
     */
    boolean addOriginalChatMessageBatch(List<ChatHistoryOriginal> chatHistoryOriginalList);

    /**
     * 根据 appId 关联删除对话记忆记录
     *
     * @param appId 应用id
     * @return 删除结果
     */
    boolean deleteByAppId(Long appId);

    /**
     * 将 APP 的对话记忆加载到缓存中
     *
     * @param appId      应用ID
     * @param chatMemory 对话记忆
     * @param maxCount   最大加载条数
     * @return 加载的条数
     */
    int loadOriginalChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);
}
