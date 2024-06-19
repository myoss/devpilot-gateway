package com.zhongan.devpilot.gateway.utils;

/**
 * Common string operations
 *
 * @author Jerry.Chen
 */
public class StringUtil {
    /**
     * 从字符串中移除双引号
     *
     * @param text 字符串
     * @return 移除双引号后的字符串
     */
    public static String removeQuote(String text) {
        if (text == null || text.length() < 2) {
            return text;
        }
        if (text.charAt(0) != '"' || text.charAt(text.length() - 1) != '"') {
            return text;
        }
        // 移除双引号(第一个和最后一个)
        return text.substring(1, text.length() - 1);
    }

    /**
     * 判断字符串是否为Markdown代码片段，并移除第一行或者最后一行
     *
     * @param input 输入的字符串
     * @return 处理后的字符串，如果不是Markdown代码片段则返回原字符串
     */
    public static String processMarkdownCodeBlock(String input) {
        // 去掉前后空白符
        if (input == null) {
            return "";
        }

        // 检查是否是Markdown代码块，前后都是三个反引号
        if (input.startsWith("```") && input.endsWith("```")) {
            // 找到第一行和最后一行的换行符位置
            int firstLineEnd = input.indexOf('\n');
            int lastLineStart = input.lastIndexOf('\n');

            // 确保有多于两行（开头和结尾的反引号必须各占一行）
            if (firstLineEnd != -1 && lastLineStart != -1 && firstLineEnd < lastLineStart) {
                // 提取中间部分的内容
                return input.substring(firstLineEnd + 1, lastLineStart);
            }
        } else if (input.startsWith("```")) {
            // 找到第一行
            int firstLineEnd = input.indexOf('\n');

            if (firstLineEnd != -1) {
                // 提取第一行之后的内容
                return input.substring(firstLineEnd + 1);
            }
        }

        // 如果不是Markdown代码块或者不能删除，则返回原始字符串
        return input;
    }

    /**
     * 移除指定的前缀，如果存在的话
     *
     * @param input  输入的字符串
     * @param prefix 查找的前缀
     * @return 移除前缀后的字符串，如果没有前缀则返回原字符串
     */
    public static String removePrefix(String input, String prefix) {
        // 检查字符串是否以指定的前缀开头
        if (input.startsWith(prefix)) {
            // 移除前缀并返回剩余部分
            return input.substring(prefix.length());
        }

        // 如果没有前缀则返回原字符串
        return input;
    }
}
