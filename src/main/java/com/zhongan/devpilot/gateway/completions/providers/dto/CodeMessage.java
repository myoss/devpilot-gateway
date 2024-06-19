package com.zhongan.devpilot.gateway.completions.providers.dto;

import lombok.Data;

/**
 * 代码补全提示信息
 *
 * @author Jerry.Chen
 */
@Data
public class CodeMessage {
    /**
     * 代码补全提示信息的唯一标识
     */
    private String id;
    /**
     * 角色
     */
    private String role;
    /**
     * 代码补全的内容
     */
    private String content;
}
