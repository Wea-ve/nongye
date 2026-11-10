package org.example.nongye.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * AI 内容安全与提示注入防护（轻量规则层，无外部依赖）
 * - 输入：长度限制、提示注入指令、越权/造假类请求
 * - 输出：禁止给出伪造溯源/篡改检测等可执行方法，防止泄露系统提示词
 * 设计为确定性规则，便于演示与审计；命中后返回统一拒答文案。
 */
@Service
public class AiSafetyService {

    private static final Logger log = LoggerFactory.getLogger(AiSafetyService.class);

    /** 单次问题最大长度，防止超长输入拖垮模型调用 */
    private static final int MAX_QUESTION_LENGTH = 1000;

    /** 提示注入 / 角色劫持特征（统一转小写后匹配） */
    private static final String[] INJECTION_PATTERNS = {
            "忽略以上", "忽略之前", "忽略前面", "无视以上", "忽略所有指令",
            "system prompt", "系统提示词", "你的系统提示", "输出你的指令", "打印你的提示",
            "repeat your instructions", "print your instructions", "ignore previous",
            "ignore above", "ignore all previous", "developer mode", "开发者模式",
            "jailbreak", "越狱模式", "你现在是", "从现在开始你是", "roleplay as",
            "<|im_start|>", "<|im_end|>", "[inst]", "### 指令", "重置你的角色"
    };

    /** 试图获取造假/篡改方法的请求 */
    private static final String[] FORBIDDEN_REQUESTS = {
            "怎么伪造溯源码", "如何伪造溯源码", "伪造溯源码", "假溯源码", "做一个假溯源码",
            "伪造检测报告", "修改检测报告", "篡改检测", "改检测结果", "删除农事记录",
            "删除检测报告", "绕过检测", "逃避检测", "怎么造假", "如何造假"
    };

    private static final String REFUSAL =
            "抱歉，我只能回答本批次的溯源信息、农事记录、检测报告和农产品安全相关问题，"
                    + "无法执行修改系统设定、伪造溯源信息或规避检测的请求。";

    /**
     * 输入安全检查
     * @return null 表示通过；非 null 为拒答文案
     */
    public String checkInput(String question) {
        if (question == null || question.isBlank()) {
            return "请输入您的问题";
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            log.warn("AI 输入超长被拦截，长度={}", question.length());
            return "问题过长，请精简到 " + MAX_QUESTION_LENGTH + " 字以内再提问。";
        }
        String lower = question.toLowerCase(Locale.ROOT);
        for (String p : INJECTION_PATTERNS) {
            if (lower.contains(p)) {
                log.warn("拦截提示注入输入：pattern={}, question={}", p, abbreviate(question));
                return REFUSAL;
            }
        }
        for (String p : FORBIDDEN_REQUESTS) {
            if (question.contains(p)) {
                log.warn("拦截违规请求：pattern={}, question={}", p, abbreviate(question));
                return REFUSAL;
            }
        }
        return null;
    }

    /**
     * 输出安全检查：模型若给出造假/篡改类可执行方法或泄露系统提示词，替换为拒答文案
     */
    public String guardOutput(String reply) {
        if (reply == null || reply.isEmpty()) return reply;
        String lower = reply.toLowerCase(Locale.ROOT);
        for (String p : FORBIDDEN_REQUESTS) {
            if (reply.contains(p)) {
                log.warn("拦截违规输出：pattern={}, reply={}", p, abbreviate(reply));
                return REFUSAL;
            }
        }
        // 系统提示词泄露防护：回答里出现内部规则原文时整体替换
        if (reply.contains("你是一个专业的农产品溯源助手") || lower.contains("system prompt")) {
            log.warn("拦截疑似系统提示词泄露：{}", abbreviate(reply));
            return REFUSAL;
        }
        return reply;
    }

    private String abbreviate(String text) {
        String oneLine = text.replace("\n", " ").trim();
        return oneLine.length() <= 80 ? oneLine : oneLine.substring(0, 80) + "…";
    }
}
