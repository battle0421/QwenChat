package org.qwen.aiqwen.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.qwen.aiqwen.assistant.TokenizationAssistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 智能分词工具 - 基于大模型的分词，支持专业术语保护
 *
 * 功能特性：
 * 1. 使用大模型进行智能分词，准确识别专业术语
 * 2. 保护专业术语（序列号、物料号、IP地址等）不被拆分
 * 3. 支持中英文混合分词
 * 4. 降级方案：大模型失败时使用规则分词
 */
@Slf4j
@Service
public class SmartTokenizer {

    @Autowired
    private TokenizationAssistant tokenizationAssistant;
    private static final ObjectMapper objectMapper = new ObjectMapper();



    // 专业术语正则模式（用于降级方案）
    private static final List<TermPattern> TERM_PATTERNS = Arrays.asList(
            // 序列号/规格型号格式: DH-STMJ03-DFJ-0P1, SN123456, abc-def-123
            new TermPattern("SERIAL_NUMBER", Pattern.compile("(?<![\\u4e00-\\u9fa5])[A-Za-z]{1,5}[-_]?[A-Za-z0-9]+(?:[-_][A-Za-z0-9]+)*(?![\\u4e00-\\u9fa5])"), 5),

            // 物料号格式: 10.2.3.4343.2302
            new TermPattern("MATERIAL_NUMBER", Pattern.compile("\\b\\d+(?:\\.\\d+){2,}\\b"), 0),

            // 带前缀的编号: NO.ABC123, ID:XYZ789, CODE:TEST-001
            new TermPattern("PREFIX_CODE", Pattern.compile("\\b(?:NO|ID|CODE|SERIAL|MODEL|TYPE)[:\\s]*[A-Za-z0-9][-A-Za-z0-9_]*\\b", Pattern.CASE_INSENSITIVE), 0),

            // IP地址: 192.168.1.1
            new TermPattern("IP_ADDRESS", Pattern.compile("\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b"), 0),

            // 版本号: v1.2.3, V2.0.1
            new TermPattern("VERSION", Pattern.compile("\\b[vV]\\d+(?:\\.\\d+)+\\b"), 0),

            // MAC地址: 00:1A:2B:3C:4D:5E
            new TermPattern("MAC_ADDRESS", Pattern.compile("\\b([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}\\b"), 0),

            // UUID: 550e8400-e29b-41d4-a716-446655440000
            new TermPattern("UUID", Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b"), 0)
    );

    /**
     * 分词主方法 - 优先使用大模型，失败时降级到规则分词
     *
     * @param text 待分词文本
     * @return 分词结果数组
     */
    public  String[] tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        // 如果大模型可用，优先使用大模型分词
        if (tokenizationAssistant != null) {
            try {
                log.info("使用大模型分词: '{}'", text);
                return tokenizeWithLlm(text);
            } catch (Exception e) {
                log.error("大模型分词失败，降级到规则分词: {}", text, e);
            }
        } else {
            log.debug("大模型未初始化，使用规则分词");
        }

        // 降级方案：使用规则分词
        return ruleBasedTokenize(text);
    }

    /**
     * 使用大模型进行智能分词
     */
    private  String[] tokenizeWithLlm(String text) {
        try {

            // 调用大模型
            String response = tokenizationAssistant.tokenizationhandler(text);
            log.debug("大模型分词原始响应: {}", response);

            // 解析 JSON 数组
            List<String> tokens = parseJsonArray(response);

            if (tokens.isEmpty()) {
                log.warn("大模型分词结果为空，降级使用规则分词");
                return ruleBasedTokenize(text);
            }

            log.info("大模型分词成功: '{}' -> {}", text, tokens);
            return tokens.toArray(new String[0]);

        } catch (Exception e) {
            log.error("大模型分词异常", e);
            throw e; // 抛出异常，由外层 tokenize 捕获并降级
        }
    }


    /**
     * 从响应中解析 JSON 数组
     */
    private static List<String> parseJsonArray(String response) {
        List<String> tokens = new ArrayList<>();

        try {
            // 提取 JSON 数组部分
            String jsonStr = extractJsonArray(response);

            if (jsonStr == null) {
                log.warn("无法从响应中提取 JSON 数组: {}", response);
                return tokens;
            }

            // 解析 JSON
            JsonNode jsonArray = objectMapper.readTree(jsonStr);

            if (!jsonArray.isArray()) {
                log.warn("响应不是 JSON 数组: {}", jsonStr);
                return tokens;
            }

            for (JsonNode node : jsonArray) {
                String token = node.asText().trim();
                if (!token.isEmpty()) {
                    tokens.add(token);
                }
            }

        } catch (Exception e) {
            log.error("解析 JSON 数组失败", e);
        }

        return tokens;
    }

    /**
     * 从文本中提取 JSON 数组
     */
    private static String extractJsonArray(String text) {
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');

        if (start == -1 || end == -1 || start >= end) {
            return null;
        }

        return text.substring(start, end + 1);
    }

    /**
     * 基于规则的分词（降级方案）
     */
    private static String[] ruleBasedTokenize(String text) {
        log.debug("使用规则分词: '{}'", text);

        try {
            // 使用 HanLP 进行基础分词
            List<String> tokens = new ArrayList<>();
            List<com.hankcs.hanlp.seg.common.Term> hanlpTerms = com.hankcs.hanlp.HanLP.segment(text);

            for (com.hankcs.hanlp.seg.common.Term term : hanlpTerms) {
                String word = term.word.trim();
                if (!word.isEmpty() && !isPunctuation(word)) {
                    tokens.add(word.toLowerCase());
                }
            }

            return tokens.toArray(new String[0]);
        } catch (Exception e) {
            log.error("规则分词失败", e);
            // 最终降级：简单分割
            return text.toLowerCase().split("\\s+");
        }
    }

    /**
     * 判断是否为标点符号
     */
    private static boolean isPunctuation(String word) {
        if (word.length() > 1) {
            return false;
        }
        char c = word.charAt(0);
        return !Character.isLetterOrDigit(c) && !Character.isIdeographic(c);
    }

    /**
     * 术语模式内部类
     */
    private static class TermPattern {
        String name;
        Pattern pattern;
        int minLength;

        TermPattern(String name, Pattern pattern, int minLength) {
            this.name = name;
            this.pattern = pattern;
            this.minLength = minLength;
        }
    }
}
