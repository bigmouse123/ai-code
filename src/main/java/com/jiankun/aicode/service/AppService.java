package com.jiankun.aicode.service;

import com.jiankun.aicode.model.dto.app.AppQueryRequest;
import com.jiankun.aicode.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.jiankun.aicode.model.entity.App;

import java.util.List;

/**
 * 应用表 服务类
 *
 * @author lijiankun
 * @since 2026/2/27
 */
public interface AppService extends IService<App> {

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
