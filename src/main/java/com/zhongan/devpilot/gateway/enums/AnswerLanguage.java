package com.zhongan.devpilot.gateway.enums;

import lombok.Getter;

import org.apache.commons.lang3.StringUtils;

@Getter
public enum AnswerLanguage {

    ENGLISH("en_US", ""),

    CHINESE("zh_CN", "\n\n请用中文回答");

    private final String code;

    private final String answerLangPrompt;

    AnswerLanguage(String code, String answerLangPrompt) {
        this.code = code;
        this.answerLangPrompt = answerLangPrompt;
    }

    public static String getAnswerLangPromptByCode(String code) {
        for (AnswerLanguage answerLanguage : AnswerLanguage.values()) {
            if (StringUtils.equalsIgnoreCase(answerLanguage.getCode(), code)) {
                return answerLanguage.getAnswerLangPrompt();
            }
        }
        return null;
    }

}
