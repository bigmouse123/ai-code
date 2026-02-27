package com.jiankun.aicode.core.saver;

import cn.hutool.core.util.StrUtil;
import com.jiankun.aicode.exception.BusinessException;
import com.jiankun.aicode.exception.ErrorCode;
import com.jiankun.aicode.model.HtmlCodeResult;
import com.jiankun.aicode.model.enums.CodeGenTypeEnum;

/**
 * HTML代码文件保存器
 *
 * @author lijiankun
 * @since 2026/2/23
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeToFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    @Override
    protected void validateInput(HtmlCodeResult result, Long appId) {
        super.validateInput(result, appId);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}

