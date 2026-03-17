package com.jiankun.aicode.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目下载服务
 *
 * @author lijiankun
 * @since 2026/3/16
 */
public interface ProjectDownloadService {
    /**
     * 下载项目为压缩包
     *
     * @param projectPath      项目路径
     * @param downloadFileName 下载文件名
     * @param response         HTTP 响应
     */
    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
