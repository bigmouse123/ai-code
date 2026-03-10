package com.jiankun.aicode.service;

import com.jiankun.aicode.model.dto.chathistory.ChatHistoryQueryRequest;
import com.jiankun.aicode.model.entity.User;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jiankun.aicode.model.entity.ChatHistory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author lijiankun
 * @since 2026/3/5
 */
public interface ChatHistoryService extends IService<ChatHistory> {

    /**
     * 新增对话历史
     *
     * @param appId       应用id
     * @param message     消息内容
     * @param messageType 消息类型
     * @param userId      用户id
     * @return 新增结果
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 根据应用ID分页查询对话历史
     *
     * @param appId          应用ID
     * @param pageSize       页面大小
     * @param lastCreateTime 最后创建时间
     * @param loginUser      登录用户
     * @return 对话历史分页数据
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 根据应用ID删除对话历史
     *
     * @param appId 应用ID
     * @return 删除结果
     */
    boolean deleteByAppId(Long appId);

    /**
     * 加载对话历史到内存
     *
     * @param appId      应用ID
     * @param chatMemory 对话记忆
     * @param maxCount   最大加载条数
     * @return 加载的条数
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest 查询请求
     * @return 查询包装类
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);
}
