package com.zhongan.devpilot.gateway.completions.context;

import lombok.Data;

/**
 * 文档上下文
 *
 * @author Jerry.Chen
 */
@Data
public class DocumentContext {
    /**
     * 文档内容
     */
    private String document;

    /**
     * 文档路径
     */
    private String path;

    /**
     * 光标位置
     */
    private Integer cursor;

    /**
     * 光标前的字符串
     */
    private String prefix;

    /**
     * 光标后的字符串
     */
    private String suffix;

    /**
     * 代码补全类型
     * 1. comment: 注释补全;
     * 2. inline: 单行补全;
     */
    private String completionType;

    /**
     * 构建文档上下文
     *
     * @param document       文档内容
     * @param path           文档路径
     * @param cursor         光标位置
     * @param completionType 代码补全类型
     * @return 文档上下文
     */
    public static DocumentContext build(String document, String path, Integer cursor, String completionType) {
        DocumentContext documentContext = new DocumentContext();
        documentContext.setDocument(document);
        documentContext.setPath(path);
        documentContext.setCursor(cursor);
        if (cursor > 1) {
            // 获取光标前的字符串
            String prefix = document.substring(0, cursor);
            String suffix = document.substring(cursor);
            documentContext.setPrefix(prefix);
            documentContext.setSuffix(suffix);
        } else {
            documentContext.setPrefix(document);
            documentContext.setSuffix("");
        }
        documentContext.setCompletionType(completionType);
        return documentContext;
    }
}
