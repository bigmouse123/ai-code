package com.jiankun.aicode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.jiankun.aicode.exception.BusinessException;
import com.jiankun.aicode.exception.ErrorCode;
import com.jiankun.aicode.model.MultiFileCodeResult;
import com.jiankun.aicode.model.enums.CodeGenTypeEnum;

/**
 * 多文件代码保存器
 *
 * @author lijiankun
 * @since 2026/2/23
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    public CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 文件
        writeToFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JavaScript 文件
        writeToFile(baseDirPath, "script.js", result.getJsCode());
    }

    @Override
    protected void validateInput(MultiFileCodeResult result, Long appId) {
        super.validateInput(result, appId);
        // 至少要有 HTML 代码，CSS 和 JS 可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
