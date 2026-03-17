package com.jiankun.aicode.service;

import com.jiankun.aicode.model.dto.app.AppAddRequest;
import com.jiankun.aicode.model.dto.app.AppQueryRequest;
import com.jiankun.aicode.model.entity.User;
import com.jiankun.aicode.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jiankun.aicode.model.entity.App;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用表 服务类
 *
 * @author lijiankun
 * @since 2026/2/27
 */
public interface AppService extends IService<App> {

    /**
     * 通过对话生成AI应用代码
     *
     * @param appId     应用ID
     * @param message   用户提示词
     * @param loginUser 登录用户
     * @return 流式响应
     */
    Flux<String> chatToGenCode(Long appId, String message, User loginUser);

    /**
     * 应用创建
     *
     * @param appAddRequest 应用创建请求
     * @param loginUser     登录用户
     * @return 应用ID
     */
    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 应用部署
     *
     * @param appId     应用ID
     * @param loginUser 登录用户
     * @return 可访问的部署地址
     */
    String deployApp(Long appId, User loginUser);

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 根据应用信息获取应用VO
     *
     * @param app 应用信息
     * @return 应用VO
     */
    AppVO getAppVO(App app);

    /**
     * 根据应用列表获取应用VO列表
     *
     * @param appList 应用列表
     * @return 应用VO列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 构造应用查询条件
     *
     * @param appQueryRequest 查询条件
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);
}
