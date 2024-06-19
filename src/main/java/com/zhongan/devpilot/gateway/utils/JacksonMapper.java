/*
 * Copyright 2018-2018 https://github.com/myoss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.zhongan.devpilot.gateway.utils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.SortedMap;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import org.springframework.util.StringUtils;

/**
 * 简单封装Jackson，实现{@code JSON String<->Java Object} 的Mapper. 封装不同的输出风格,
 * 使用不同的builder函数创建实例.
 * <a href="https://github.com/myoss-cloud/myoss-starter-projects/blob/master/myoss-starter-core/src/main/java/app/myoss/cloud/core/utils/JacksonMapper.java">JacksonMapper.java</a>
 *
 * @author Jerry.Chen
 * @since 2018年5月4日 下午6:43:13
 */
public class JacksonMapper {
    /**
     * {@link SortedMap} 类型转换器
     */
    public static TypeReference<SortedMap<String, String>> SORTED_MAP_S2S_TYPE_REFERENCE;
    /**
     * {@link LinkedHashMap} 类型转换器
     */
    public static TypeReference<LinkedHashMap<String, Object>> LINKED_HASH_MAP_S2O_TYPE_REFERENCE;

    static {
        SORTED_MAP_S2S_TYPE_REFERENCE = new TypeReference<>() {
        };
        LINKED_HASH_MAP_S2O_TYPE_REFERENCE = new TypeReference<>() {
        };
    }

    @Getter
    private ObjectMapper mapper;

    /**
     * 创建 Jackson Mapper 对象
     */
    public JacksonMapper() {
        this(null);
    }

    /**
     * 创建 Jackson Mapper 对象
     *
     * @param include 属性的风格
     */
    public JacksonMapper(Include include) {
        mapper = new ObjectMapper();
        // 设置输出时包含属性的风格
        if (include != null) {
            mapper.setSerializationInclusion(include);
        }
        // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 创建只输出非Null的属性到Json字符串的Mapper,建议在外部接口中使用.
     *
     * @return JacksonMapper
     */
    public static JacksonMapper nonNullMapper() {
        return new JacksonMapper(Include.NON_NULL);
    }

    /**
     * 创建只输出非Null且非Empty(如List.isEmpty)的属性到Json字符串的Mapper,建议在外部接口中使用.
     *
     * @return JacksonMapper
     */
    public static JacksonMapper nonEmptyMapper() {
        return new JacksonMapper(Include.NON_EMPTY);
    }

    /**
     * 创建只输出初始值被改变的属性到Json字符串的Mapper, 最节约的存储方式，建议在内部接口中使用。
     *
     * @return JacksonMapper
     */
    public static JacksonMapper nonDefaultMapper() {
        return new JacksonMapper(Include.NON_DEFAULT);
    }

    /**
     * Object可以是POJO，也可以是Collection或数组。 如果对象为Null, 返回"null". 如果集合为空集合, 返回"[]".
     *
     * @param object Java对象
     * @return json字符串
     */
    public String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("write to json string error:" + object, e);
        }
    }

    /**
     * 反序列化POJO或简单Collection如 {@code List<String>}. 如果JSON字符串为Null或"null"字符串,
     * 返回Null. 如果JSON字符串为"[]", 返回空集合. 如需反序列化复杂Collection如 {@code List<MyBean>},
     * 请使用fromJson(String, JavaType)
     *
     * @param jsonString json字符串
     * @param clazz      class
     * @param <T>        泛型
     * @return POJO对象
     * @see #fromJson(String, JavaType)
     */
    public <T> T fromJson(String jsonString, Class<T> clazz) {
        if (!StringUtils.hasText(jsonString)) {
            return null;
        }
        try {
            return mapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            throw new RuntimeException("parse json string error:" + jsonString, e);
        }
    }

    /**
     * 反序列化复杂Collection如 {@code List<Bean>},
     * 先使用createCollectionType()或constructMapType()构造类型, 然后调用本函数.
     *
     * @param jsonString json字符串
     * @param javaType   JavaType
     * @param <T>        泛型
     * @return POJO对象
     */
    public <T> T fromJson(String jsonString, JavaType javaType) {
        if (!StringUtils.hasText(jsonString)) {
            return null;
        }
        try {
            return mapper.readValue(jsonString, javaType);
        } catch (IOException e) {
            throw new RuntimeException("parse json string error:" + jsonString, e);
        }
    }

    /**
     * 反序列化为Map对象
     *
     * @param jsonString    json字符串
     * @param typeReference 目标类型
     * @param <T>           泛型
     * @return Map对象
     */
    public <T> T fromJson(String jsonString, TypeReference<T> typeReference) {
        if (!StringUtils.hasText(jsonString)) {
            return null;
        }
        try {
            return mapper.readValue(jsonString, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("parse json string error:" + jsonString, e);
        }
    }
}