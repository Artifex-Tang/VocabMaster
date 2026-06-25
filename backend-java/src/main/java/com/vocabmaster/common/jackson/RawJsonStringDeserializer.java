package com.vocabmaster.common.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ValueNode;

import java.io.IOException;

/**
 * 把任意 JSON token 还原成字符串字段。
 *
 * <p>用途：{@code related_words} 这类字段在序列化时用 {@code @JsonRawValue} 把 JSON 字符串
 * 当原始 JSON 对象写出（前端拿到的是对象而非转义字符串）。但当对象经 Redis
 * {@code Jackson2JsonRedisSerializer<Object>}（无类型信息）存取再 {@code convertValue} 还原时，
 * 该字段读回的是 {@code LinkedHashMap}（对象）或 {@code ArrayList}（数组），无法直接映射回
 * {@code String} 会抛 {@code MismatchedInputException}。本反序列化器接受值/对象/数组任一形态，
 * 统一回填为紧凑 JSON 字符串，让模型能安全往返 Redis。
 */
public class RawJsonStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        TreeNode node = p.readValueAsTree();
        if (node == null || node.isMissingNode()) {
            return null;
        }
        if (node.isValueNode()) {
            // 已是普通字符串/标量：原样取文本（null 字面量 → null）
            ValueNode vn = (ValueNode) node;
            return vn.isNull() ? null : vn.asText();
        }
        // 对象/数组：序列化回紧凑 JSON 字符串
        return node.toString();
    }
}
