package com.zhongan.devpilot.gateway.completions.providers.dto;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 代码前缀组件
 *
 * @author Jerry.Chen
 */
@AllArgsConstructor
@Data
public class CodePrefixComponents {
    /**
     * 头部
     */
    private CodeTrimmed head;
    /**
     * 尾部
     */
    private CodeTrimmed tail;
    /**
     * 重叠部分
     */
    private String overlap;

    public CodePrefixComponents(CodeTrimmed head, CodeTrimmed tail) {
        this.head = head;
        this.tail = tail;
    }

    @Data
    @AllArgsConstructor
    public static class CodeTrimmed {
        private String raw;
        private String trimmed;
        private String leadSpace;
        private String rearSpace;
    }

    public static CodePrefixComponents getHeadAndTail(String source) {
        String[] lines = source.split("\n");
        int tailThreshold = 2;

        int nonEmptyCount = 0;
        int tailStart = -1;
        for (int i = lines.length - 1; i >= 0; i--) {
            if (!lines[i].trim().isEmpty()) {
                nonEmptyCount++;
            }
            if (nonEmptyCount >= tailThreshold) {
                tailStart = i;
                break;
            }
        }

        CodePrefixComponents headAndTail;
        if (tailStart == -1) {
            headAndTail = new CodePrefixComponents(trimSpace(source), trimSpace(source), source);
        } else {
            String head = String.join("\n", Arrays.copyOfRange(lines, 0, tailStart)) + "\n";
            String tail = String.join("\n", Arrays.copyOfRange(lines, tailStart, lines.length));
            headAndTail = new CodePrefixComponents(trimSpace(head), trimSpace(tail));
        }

        // We learned that Anthropic is giving us worse results with trailing whitespace in the prompt.
        // To fix this, we started to trim the prompt.
        //
        // However, when the prefix includes a line break, the LLM needs to know that we do not want the
        // current line to complete and instead start a new one. For this specific case, we're injecting
        // a line break in the trimmed prefix.
        //
        // This will only be added if the existing line is otherwise empty and will help especially with
        // cases like users typing a comment and asking the LLM to provide a suggestion for the next
        // line of code:
        //
        //     // Write some code
        //     █
        //
        if (headAndTail.getTail().getRearSpace().contains("\n")) {
            headAndTail.getTail().setTrimmed(headAndTail.getTail().getTrimmed() + "\n");
        }

        return headAndTail;
    }

    private static CodeTrimmed trimSpace(String text) {
        String trimmed = text.trim();
        int headEnd = text.indexOf(trimmed);
        String leadSpace = text.substring(0, headEnd);
        String rearSpace = text.substring(headEnd + trimmed.length());
        return new CodeTrimmed(text, trimmed, leadSpace, rearSpace);
    }
}
